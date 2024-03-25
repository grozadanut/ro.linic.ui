package ro.linic.ui.security.model;

import java.util.Collection;

/**
 * An {@link org.springframework.security.core.Authentication} implementation that is
 * designed for simple presentation of a username and password.
 * <p>
 * The <code>principal</code> and <code>credentials</code> should be set with an
 * <code>Object</code> that provides the respective property via its
 * <code>Object.toString()</code> method. The simplest such <code>Object</code> to use is
 * <code>String</code>.
 *
 * @author Ben Alex
 * @author Norbert Nowak
 */
public class UsernamePasswordAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private final String principal;
	private String credentials;

	/**
	 * This constructor can be safely used by any code that wishes to create a
	 * <code>UsernamePasswordAuthenticationToken</code>, as the {@link #isAuthenticated()}
	 * will return <code>false</code>.
	 *
	 */
	private UsernamePasswordAuthenticationToken(final String principal, final String credentials) {
		super(null);
		this.principal = principal;
		this.credentials = credentials;
		setAuthenticated(false);
	}

	/**
	 * This constructor should only be used by <code>AuthenticationManager</code> or
	 * <code>AuthenticationProvider</code> implementations that are satisfied with
	 * producing a trusted (i.e. {@link #isAuthenticated()} = <code>true</code>)
	 * authentication token.
	 * @param principal
	 * @param credentials
	 * @param authorities
	 */
	private UsernamePasswordAuthenticationToken(final String principal, final String credentials,
			final Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.principal = principal;
		this.credentials = credentials;
		super.setAuthenticated(true); // must use super, as we override
	}

	/**
	 * This factory method can be safely used by any code that wishes to create a
	 * unauthenticated <code>UsernamePasswordAuthenticationToken</code>.
	 * @param principal
	 * @param credentials
	 * @return UsernamePasswordAuthenticationToken with false isAuthenticated() result
	 *
	 * @since 5.7
	 */
	public static UsernamePasswordAuthenticationToken unauthenticated(final String principal, final String credentials) {
		return new UsernamePasswordAuthenticationToken(principal, credentials);
	}

	/**
	 * This factory method can be safely used by any code that wishes to create a
	 * authenticated <code>UsernamePasswordAuthenticationToken</code>.
	 * @param principal
	 * @param credentials
	 * @return UsernamePasswordAuthenticationToken with true isAuthenticated() result
	 *
	 * @since 5.7
	 */
	public static UsernamePasswordAuthenticationToken authenticated(final String principal, final String credentials,
			final Collection<? extends GrantedAuthority> authorities) {
		return new UsernamePasswordAuthenticationToken(principal, credentials, authorities);
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
			throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
		super.setAuthenticated(false);
	}
}
