package ro.linic.ui.security.model;

import java.io.Serializable;
import java.util.Collection;

/**
 * Represents an anonymous <code>Authentication</code>.
 */
public class AnonymousAuthenticationToken extends AbstractAuthenticationToken implements Serializable {

	private static final long serialVersionUID = 1L;

	private AnonymousAuthenticationToken() {
		super(null);
		setAuthenticated(false);
	}
	
	private AnonymousAuthenticationToken(final Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		setAuthenticated(true);
	}
	
	/**
	 * This factory method can be safely used by any code that wishes to create a
	 * unauthenticated <code>AnonymousAuthenticationToken</code>.
	 * @return AnonymousAuthenticationToken with false isAuthenticated() result
	 */
	public static AnonymousAuthenticationToken unauthenticated() {
		return new AnonymousAuthenticationToken();
	}

	/**
	 * This factory method can be safely used by any code that wishes to create an
	 * authenticated <code>AnonymousAuthenticationToken</code>.
	 * @return AnonymousAuthenticationToken with true isAuthenticated() result
	 */
	public static AnonymousAuthenticationToken authenticated(final Collection<? extends GrantedAuthority> authorities) {
		return new AnonymousAuthenticationToken(authorities);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		return obj instanceof AnonymousAuthenticationToken;
	}

	/**
	 * Always returns an empty <code>String</code>
	 * @return an empty String
	 */
	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public String getName() {
		return "Anonymous";
	}
}
