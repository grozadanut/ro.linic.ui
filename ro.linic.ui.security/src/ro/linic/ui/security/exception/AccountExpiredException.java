package ro.linic.ui.security.exception;

/**
 * Thrown if an authentication request is rejected because the account has expired. Makes
 * no assertion as to whether or not the credentials were valid.
 *
 * @author Ben Alex
 */
public class AccountExpiredException extends AccountStatusException {

	/**
	 * Constructs a <code>AccountExpiredException</code> with the specified message.
	 * @param msg the detail message
	 */
	public AccountExpiredException(final String msg) {
		super(msg);
	}

	/**
	 * Constructs a <code>AccountExpiredException</code> with the specified message and
	 * root cause.
	 * @param msg the detail message
	 * @param cause root cause
	 */
	public AccountExpiredException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
