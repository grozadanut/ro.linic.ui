package ro.linic.ui.security.services;

import ro.linic.ui.security.exception.AuthenticationException;
import ro.linic.ui.security.exception.BadCredentialsException;
import ro.linic.ui.security.exception.DisabledException;
import ro.linic.ui.security.exception.LockedException;
import ro.linic.ui.security.model.Authentication;
import ro.linic.ui.security.model.RestoreAuthenticationToken;

/**
 * Processes an {@link Authentication} request.
 */
public interface AuthenticationManager {
	/**
	 * Attempts to authenticate the passed {@link Authentication} object, returning a
	 * fully populated <code>Authentication</code> object (including granted authorities)
	 * if successful.
	 * <p>
	 * An <code>AuthenticationManager</code> must honour the following contract concerning
	 * exceptions:
	 * <ul>
	 * <li>A {@link DisabledException} must be thrown if an account is disabled and the
	 * <code>AuthenticationManager</code> can test for this state.</li>
	 * <li>A {@link LockedException} must be thrown if an account is locked and the
	 * <code>AuthenticationManager</code> can test for account locking.</li>
	 * <li>A {@link BadCredentialsException} must be thrown if incorrect credentials are
	 * presented. Whilst the above exceptions are optional, an
	 * <code>AuthenticationManager</code> must <B>always</B> test credentials.</li>
	 * <li>Implementors must support {@link RestoreAuthenticationToken} which is passed
	 * when the session is restored with the principal and credentials injected, and should 
	 * return a full authentication, including granted authorities. This token is used when 
	 * changing some application properties that require restart, so usually the user is not 
	 * prompted to login again.</li>
	 * </ul>
	 * Exceptions should be tested for and if applicable thrown in the order expressed
	 * above (i.e. if an account is disabled or locked, the authentication request is
	 * immediately rejected and the credentials testing process is not performed). This
	 * prevents credentials being tested against disabled or locked accounts.
	 * @param authentication the authentication request object, can be null
	 * @return a fully authenticated object including credentials
	 * @throws AuthenticationException if authentication fails
	 */
	Authentication authenticate(Authentication authentication) throws AuthenticationException;
}