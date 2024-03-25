package ro.linic.ui.security.exception;

/**
 * Thrown if an authentication request is rejected because the account's credentials have
 * expired. Makes no assertion as to whether or not the credentials were valid.
 *
 * @author Ben Alex
 */
public class CredentialsExpiredException extends AccountStatusException {

	/**
	 * Constructs a <code>CredentialsExpiredException</code> with the specified message.
	 * @param msg the detail message
	 */
	public CredentialsExpiredException(final String msg) {
		super(msg);
	}

	/**
	 * Constructs a <code>CredentialsExpiredException</code> with the specified message
	 * and root cause.
	 * @param msg the detail message
	 * @param cause root cause
	 */
	public CredentialsExpiredException(final String msg, final Throwable cause) {
		super(msg, cause);
	}
}
