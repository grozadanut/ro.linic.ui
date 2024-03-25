package ro.linic.ui.security.model;

/**
 * An {@link Authentication} implementation that is
 * designed for restoring the authentication after an application restart.
 * <p>
 */
public class RestoreAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private final String principal;
	private String credentials;

	/**
	 * This constructor can be safely used by any code that wishes to create a
	 * <code>RestoreAuthenticationToken</code>, as the {@link #isAuthenticated()}
	 * will return <code>false</code>.
	 *
	 */
	private RestoreAuthenticationToken(final String principal, final String credentials) {
		super(null);
		this.principal = principal;
		this.credentials = credentials;
		setAuthenticated(false);
	}

	/**
	 * This factory method can be safely used by any code that wishes to create an
	 * unauthenticated <code>RestoreAuthenticationToken</code>.
	 * @param principal
	 * @param credentials
	 * @return RestoreAuthenticationToken with false isAuthenticated() result
	 */
	public static RestoreAuthenticationToken unauthenticated(final String principal, final String credentials) {
		return new RestoreAuthenticationToken(principal, credentials);
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
	public void setAuthenticated(final boolean isAuthenticated) throws IllegalArgumentException {
		if(isAuthenticated)
			throw new IllegalArgumentException("Cannot set this token to trusted");
		super.setAuthenticated(false);
	}
}
