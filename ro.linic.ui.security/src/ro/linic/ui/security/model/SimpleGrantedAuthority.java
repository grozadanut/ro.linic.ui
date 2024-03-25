package ro.linic.ui.security.model;

import ro.linic.ui.security.util.StringUtils;

/**
 * Basic concrete implementation of a {@link GrantedAuthority}.
 *
 * <p>
 * Stores a {@code String} representation of an authority granted to the
 * {@link org.springframework.security.core.Authentication Authentication} object.
 *
 * @author Luke Taylor
 */
public final class SimpleGrantedAuthority implements GrantedAuthority {

	private static final long serialVersionUID = 1;

	private final String role;

	public SimpleGrantedAuthority(final String role) {
		if (!StringUtils.hasText(role))
			throw new IllegalArgumentException("A granted authority textual representation is required");
		this.role = role;
	}

	@Override
	public String getAuthority() {
		return this.role;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof SimpleGrantedAuthority) {
			return this.role.equals(((SimpleGrantedAuthority) obj).role);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.role.hashCode();
	}

	@Override
	public String toString() {
		return this.role;
	}
}
