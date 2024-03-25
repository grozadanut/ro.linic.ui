package ro.linic.ui.security.services;

import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.model.Authentication;

public interface AuthenticationSession {
	void restartPreservingSession();
	/**
	 * Returns the {@link Authentication} for this session. If the Authentication is null or Authentication.isAuthenticated() 
	 * returns false, it forces the authentication using the {@link AuthenticationManager}, else the AuthenticationManager 
	 * is not called anymore, implying the authentication is still valid.
	 * 
	 * @return the {@link Authentication} for this session, never null
	 */
	Authentication authentication() throws AuthenticationException;
}
