package ro.linic.ui.security.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Base class for <code>Authentication</code> objects.
 * <p>
 * Implementations which use this class should be immutable.
 *
 * @author Ben Alex
 * @author Luke Taylor
 */
public abstract class AbstractAuthenticationToken implements Authentication {

	private final Collection<GrantedAuthority> authorities;
	private boolean authenticated = false;

	/**
	 * Creates a token with the supplied array of authorities.
	 * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
	 * represented by this authentication object.
	 */
	public AbstractAuthenticationToken(final Collection<? extends GrantedAuthority> authorities) {
		if (authorities == null) {
			this.authorities = Collections.emptyList();
			return;
		}
		for (final GrantedAuthority a : authorities) {
			Objects.requireNonNull(a, "Authorities collection cannot contain any null elements");
		}
		this.authorities = Collections.unmodifiableList(new ArrayList<>(authorities));
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@Override
	public boolean isAuthenticated() {
		return this.authenticated;
	}

	@Override
	public void setAuthenticated(final boolean authenticated) {
		this.authenticated = authenticated;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AbstractAuthenticationToken)) {
			return false;
		}
		final AbstractAuthenticationToken test = (AbstractAuthenticationToken) obj;
		if (!this.authorities.equals(test.authorities)) {
			return false;
		}
		if ((this.getCredentials() == null) && (test.getCredentials() != null)) {
			return false;
		}
		if ((this.getCredentials() != null) && !this.getCredentials().equals(test.getCredentials())) {
			return false;
		}
		if (this.getName() == null && test.getName() != null) {
			return false;
		}
		if (this.getName() != null && !this.getName().equals(test.getName())) {
			return false;
		}
		return this.isAuthenticated() == test.isAuthenticated();
	}

	@Override
	public int hashCode() {
		int code = 31;
		for (final GrantedAuthority authority : this.authorities) {
			code ^= authority.hashCode();
		}
		if (this.getName() != null) {
			code ^= this.getName().hashCode();
		}
		if (this.getCredentials() != null) {
			code ^= this.getCredentials().hashCode();
		}
		if (this.isAuthenticated()) {
			code ^= -37;
		}
		return code;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(" [");
		sb.append("Principal=").append(getName()).append(", ");
		sb.append("Credentials=[PROTECTED], ");
		sb.append("Authenticated=").append(isAuthenticated()).append(", ");
		sb.append("Granted Authorities=").append(this.authorities);
		sb.append("]");
		return sb.toString();
	}

}
