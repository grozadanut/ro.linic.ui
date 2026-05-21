package ro.linic.ui.legacy;

import static ro.flexbiz.util.commons.StringUtils.isEmpty;

import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.prefs.BackingStoreException;

import ro.colibri.entities.user.User;
import ro.linic.ui.base.services.model.GenericValue;
import ro.linic.ui.base.services.util.UIUtils;
import ro.linic.ui.http.BodyProvider;
import ro.linic.ui.http.HttpHeaders;
import ro.linic.ui.http.HttpUtils;
import ro.linic.ui.http.RestCaller;
import ro.linic.ui.legacy.dialogs.security.LoginDialog;
import ro.linic.ui.legacy.dialogs.security.TwoFactorCodeDialog;
import ro.linic.ui.legacy.mapper.PermissionMapper;
import ro.linic.ui.legacy.session.BusinessDelegate;
import ro.linic.ui.legacy.session.ClientSession;
import ro.linic.ui.legacy.session.ServiceLocator;
import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.model.AnonymousAuthenticationToken;
import ro.linic.ui.security.model.Authentication;
import ro.linic.ui.security.model.RestoreAuthenticationToken;
import ro.linic.ui.security.model.UsernamePasswordAuthenticationToken;
import ro.linic.ui.security.services.AuthenticationManager;

@Component
public class LegacyAuthenticationManager implements AuthenticationManager {
	private static final ILog log = ILog.of(LegacyAuthenticationManager.class);
	
	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		if (authentication != null && authentication instanceof RestoreAuthenticationToken)
			return restoreAuth((RestoreAuthenticationToken) authentication);
		
		if (authentication == null || !authentication.isAuthenticated())
			return login();
		
		return authentication;
	}

	private static boolean opened = false; // avoid stack overflow error when called from BetaTester.evaluate
	private Authentication login() {
		if (opened)
			return AnonymousAuthenticationToken.unauthenticated();
		
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(bundle.getSymbolicName());
		final IEclipseContext ctx = EclipseContextFactory.getServiceContext(getClass()).getActiveLeaf();
		
		while (!ClientSession.instance().isLoggedIn())
		{
			final LoginDialog dialog = new LoginDialog(null, prefs, ctx);

			opened = true;
			if (dialog.open() != Window.OK)
				System.exit(0);
			opened = false;
		}
		// MOQUI login to get session token
		String sessionId = null, csrf = null;
		if (!isEmpty(UIUtils.moquiBaseUrl())) {
			final Optional<HttpResponse<String>> loginResponse = RestCaller.post(UIUtils.moquiBaseUrl()+"/rest/login")
					.addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.addHeader(HttpHeaders.ACCEPT, "application/json")
					.body(BodyProvider.of(GenericValue.of("", "", "username", ClientSession.instance().getUsername(),
							"password",  ClientSession.instance().getPassword())))
					.syncRaw(BodyHandlers.ofString(), t -> log.error(t.getMessage(), t))
					.map(res -> readLoginResponse(res, ctx));
			if (loginResponse.isPresent()) {
				csrf = loginResponse.get().headers().firstValue("x-csrf-token").orElse(null);
				sessionId = loginResponse.get().headers()
						.firstValue("set-cookie")
						.map(cookie -> {
							final Matcher matcher = Pattern.compile("JSESSIONID=([^;]+)").matcher(cookie);
							return matcher.find() ? matcher.group(1) : null;
						})
						.orElse(null);
			}
		}
		return UsernamePasswordAuthenticationToken.authenticated(ClientSession.instance().getUsername(), ClientSession.instance().getPassword(),
				sessionId, csrf,
				PermissionMapper.toGrantedAuthorities(ClientSession.instance().getLoggedUser().getRole().getPermissions()));
	}
	
	private HttpResponse<String> readLoginResponse(final HttpResponse<String> res, final IEclipseContext ctx) {
		final GenericValue response = HttpUtils.fromJSON(res.body(), GenericValue.class);
		if (response.getBoolean("loggedIn"))
			return res;
		
		if (response.getBoolean("secondFactorRequired")) {
			final TwoFactorCodeDialog twoFactordialog = new TwoFactorCodeDialog(Display.getCurrent().getActiveShell(), ctx, response);
			if (twoFactordialog.open() != Window.OK)
				System.exit(0);
			return twoFactordialog.response();
		}
		
		log.error(UIUtils.moquiBaseUrl()+"/rest/login RESPONSE: "+res.body());
		return null;
	}
	
	private Authentication restoreAuth(final RestoreAuthenticationToken authentication) {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(bundle.getSymbolicName());
		
		ServiceLocator.clearCache();
		ClientSession.instance().setUsername(authentication.getName());
		ClientSession.instance().setPassword(authentication.getCredentials());
		final User user = ClientSession.instance().login();
		
		if (user != null)
		{
			prefs.put(LoginDialog.DB_USERS_PROP, LoginDialog.toProp(BusinessDelegate.usersWithCompanyRoles()));
			prefs.put(LoginDialog.DB_COMPANIES_PROP, LoginDialog.toPropComp(BusinessDelegate.companiesWithGestiuni()));
			try {
				prefs.flush();
			} catch (final BackingStoreException e) {
				log.error(e.getMessage(), e);
			}
			return UsernamePasswordAuthenticationToken.authenticated(ClientSession.instance().getUsername(),
					ClientSession.instance().getPassword(), authentication.getSessionId(), authentication.getCsrf(),
					PermissionMapper.toGrantedAuthorities(ClientSession.instance().getLoggedUser().getRole().getPermissions()));
		}
		return AnonymousAuthenticationToken.unauthenticated();
	}
}
