package ro.linic.ui.security.exception;

import ro.linic.ui.security.model.Authentication;

/**
 * Abstract superclass for all exceptions related to an {@link Authentication} object
 * being invalid for whatever reason.
 *
 * @author Ben Alex
 */
public abstract class AuthenticationException extends RuntimeException {

	/**
	 * Constructs an {@code AuthenticationException} with the specified message and root
	 * cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public AuthenticationException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructs an {@code AuthenticationException} with the specified message and no
	 * root cause.
	 * @param msg the detail message
	 */
	public AuthenticationException(final String msg) {
		super(msg);
	}

}
