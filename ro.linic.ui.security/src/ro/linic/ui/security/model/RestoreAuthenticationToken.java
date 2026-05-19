package ro.linic.ui.security.model;

/**
 * An {@link Authentication} implementation that is
 * designed for restoring the authentication after an application restart.
 * <p>
 */
public class RestoreAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private final String principal;
	private final String credentials;
	private final String sessionId;
	private final String csrf;

	/**
	 * This constructor can be safely used by any code that wishes to create a
	 * <code>RestoreAuthenticationToken</code>, as the {@link #isAuthenticated()}
	 * will return <code>false</code>.
	 *
	 */
	private RestoreAuthenticationToken(final String principal, final String credentials, final String sessionId, final String csrf) {
		super(null);
		this.principal = principal;
		this.credentials = credentials;
		this.sessionId = sessionId;
		this.csrf = csrf;
		setAuthenticated(false);
	}

	/**
	 * This factory method can be safely used by any code that wishes to create an
	 * unauthenticated <code>RestoreAuthenticationToken</code>.
	 * @param principal
	 * @param credentials
	 * @return RestoreAuthenticationToken with false isAuthenticated() result
	 */
	public static RestoreAuthenticationToken unauthenticated(final String principal, final String credentials, final String sessionId, final String csrf) {
		return new RestoreAuthenticationToken(principal, credentials, sessionId, csrf);
	}

	@Override
	public String getCredentials() {
		return this.credentials;
	}

	@Override
	public String getName() {
		return (this.principal == null) ? "" : this.principal;
	}
	
	@Override
	public String getSessionId() {
		return sessionId;
	}
	
	@Override
	public String getCsrf() {
		return csrf;
	}

	@Override
	public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {
		if(isAuthenticated)
			throw new IllegalArgumentException("Cannot set this token to trusted");
		super.setAuthenticated(false);
	}
}
