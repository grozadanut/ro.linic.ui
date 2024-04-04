package ro.linic.ui.security.services;

import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.model.Authentication;

public interface AuthenticationSession {
	/**
	 * If an authenticated session exists, it stores it to a secured file, and it reloads it in memory after 
	 * an application restart. Usually used when you need to change some properties that require a restart and 
	 * you don't want the user to login again.
	 * </br></br>
	 * NOTE: the session is invalidated after a short period of time, 
	 * so make sure you restart early after calling this method.
	 */
	void storeSession();
	/**
	 * Returns the {@link Authentication} for this session. If the Authentication is null or Authentication.isAuthenticated() 
	 * returns false, it forces the authentication using the {@link AuthenticationManager}, else the AuthenticationManager 
	 * is not called anymore, implying the authentication is still valid.
	 * 
	 * @return the {@link Authentication} for this session, never null
	 */
	Authentication authentication() throws AuthenticationException;
	
	/**
	 * Authentication can be forced by calling {@link AuthenticationSession.authentication()}
	 * 
	 * @return whether the user is authenticated or not
	 */
	boolean isAuthenticated();
}
