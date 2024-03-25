package ro.linic.ui.security.exception;

/**
 * Base class for authentication exceptions which are caused by a particular user account
 * status (locked, disabled etc).
 *
 * @author Luke Taylor
 */
public abstract class AccountStatusException extends AuthenticationException {

	public AccountStatusException(final String msg) {
		super(msg);
	}

	public AccountStatusException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

}
