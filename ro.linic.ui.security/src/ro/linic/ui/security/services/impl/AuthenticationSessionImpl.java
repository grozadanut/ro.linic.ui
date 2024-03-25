package ro.linic.ui.security.services.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.model.AnonymousAuthenticationToken;
import ro.linic.ui.security.model.Authentication;
import ro.linic.ui.security.model.RestoreAuthenticationToken;
import ro.linic.ui.security.services.AuthenticationManager;
import ro.linic.ui.security.services.AuthenticationSession;

@Component(immediate = true, service = {AuthenticationSession.class, EventHandler.class}, 
property = EventConstants.EVENT_TOPIC + "=" + UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class AuthenticationSessionImpl implements AuthenticationSession, EventHandler {
	private static final Logger log = Logger.getLogger(AuthenticationSessionImpl.class.getName());
	
	private static final String SAVE_TIME_KEY = "save_time";
	private static final String PRINCIPAL_KEY = "principal";
	private static final String CREDENTIALS_KEY = "credentials";
	private static final Duration SESSION_INVALIDATE_DURATION = Duration.ofSeconds(60);
	
	private IWorkbench workbench;
	private AuthenticationManager authManager;
	
	private Authentication authentication;
	private boolean restartWhenReady = false;
	
	@Override
	public Authentication authentication() throws AuthenticationException {
		if (authentication == null || !authentication.isAuthenticated())
			authentication = authManager == null ?
					AnonymousAuthenticationToken.unauthenticated() :
						authManager.authenticate(authentication);

		return authentication;
	}
	
	@Activate
	private void activate() {
		restoreSessionAndClear();
	}
	
	private void restoreSessionAndClear() {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final ISecurePreferences root = SecurePreferencesFactory.getDefault();
 		final ISecurePreferences node = root.node(bundle.getSymbolicName());
 		
 		try {
			final String saveTime = node.get(SAVE_TIME_KEY, null);
			
			if (saveTime != null) {
				final Instant savedAt = Instant.parse(saveTime);
				if (Instant.now().isBefore(savedAt.plus(SESSION_INVALIDATE_DURATION))) {
					final String principal = node.get(PRINCIPAL_KEY, null);
					final String credentials = node.get(CREDENTIALS_KEY, null);
					authentication = RestoreAuthenticationToken.unauthenticated(principal, credentials);
				}
			}
		} catch (final StorageException e) {
			log.log(Level.SEVERE, "Error getting secure preferences", e);
		}
 		
 		try {
			node.remove(SAVE_TIME_KEY);
			node.remove(PRINCIPAL_KEY);
			node.remove(CREDENTIALS_KEY);
			node.flush();
		} catch (final IOException e) {
			log.log(Level.SEVERE, "Error removing secure preferences", e);
		}
	}

	@Override
	public void restartPreservingSession() {
		final Authentication auth = authentication();
		if (auth.isAuthenticated())
			storeSession(auth);
		
		restart();
	}

	private void storeSession(final Authentication auth) {
		final Bundle bundle = FrameworkUtil.getBundle(getClass());
		final ISecurePreferences root = SecurePreferencesFactory.getDefault();
 		final ISecurePreferences node = root.node(bundle.getSymbolicName());
 		
		try {
			node.put(SAVE_TIME_KEY, Instant.now().toString(), true);
			node.put(PRINCIPAL_KEY, auth.getName(), true);
			node.put(CREDENTIALS_KEY, auth.getCredentials().toString(), true);
			node.flush();
		} catch (final IOException | StorageException e) {
			log.log(Level.SEVERE, "Error storing secure preferences", e);
		}
	}

	private void restart() {
		if (workbench != null)
			Display.getDefault().execute(() -> workbench.restart());
		else
			restartWhenReady = true;
	}
	
	@Override
    public void handleEvent(final Event event) {
		if (restartWhenReady)
			Job.create("Restart", (ICoreRunnable) monitor -> Display.getDefault().syncExec(() -> workbench.restart()))
			.schedule(1);
		restartWhenReady = false;
    }
	
	@Reference(
            service = IWorkbench.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC
    )
    private void setWorkbench(final IWorkbench workbench) {
        this.workbench = workbench;
	}

	@SuppressWarnings("unused")
	private void unsetWorkbench(final IWorkbench workbench) {
		this.workbench = null;
	}
	
	@Reference(
            service = AuthenticationManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC
    )
    private void setAuthenticationManager(final AuthenticationManager authManager) {
        this.authManager = authManager;
	}

	@SuppressWarnings("unused")
	private void unsetAuthenticationManager(final AuthenticationManager authManager) {
		this.authManager = null;
	}
}
