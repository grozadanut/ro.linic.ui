package ro.linic.ui.legacy;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.window.Window;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.prefs.BackingStoreException;

import ro.colibri.entities.user.User;
import ro.linic.ui.legacy.dialogs.LoginDialog;
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
	private static final Logger log = Logger.getLogger(LegacyAuthenticationManager.class.getName());
	
	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		if (authentication == null)
			return login();
		else if (authentication instanceof RestoreAuthenticationToken)
			return restoreAuth((RestoreAuthenticationToken) authentication);
		
		return authentication;
	}

	private Authentication login() {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(bundle.getSymbolicName());
		IEclipseContext ctx = EclipseContextFactory.getServiceContext(getClass());
		ctx = ctx.getActiveLeaf();
		
		while (!ClientSession.instance().isLoggedIn())
		{
			final LoginDialog dialog = new LoginDialog(null, prefs, ctx);

			if (dialog.open() != Window.OK)
				System.exit(0);
		}
		return UsernamePasswordAuthenticationToken.authenticated(ClientSession.instance().getUsername(), ClientSession.instance().getPassword(),
				PermissionMapper.toGrantedAuthorities(ClientSession.instance().getLoggedUser().getRole().getPermissions()));
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
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return UsernamePasswordAuthenticationToken.authenticated(ClientSession.instance().getUsername(),
					ClientSession.instance().getPassword(),
					PermissionMapper.toGrantedAuthorities(ClientSession.instance().getLoggedUser().getRole().getPermissions()));
		}
		return AnonymousAuthenticationToken.unauthenticated();
	}
}
