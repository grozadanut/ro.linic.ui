package ro.linic.ui.legacy.mapper;

import static ro.colibri.util.ListUtils.toImmutableList;

import java.util.Collection;

import ro.colibri.entities.user.Permission;
import ro.linic.ui.security.model.GrantedAuthority;
import ro.linic.ui.security.model.SimpleGrantedAuthority;

public class PermissionMapper {
	public static Collection<GrantedAuthority> toGrantedAuthorities(final Collection<Permission> permission) {
		return permission.stream()
				.map(p -> new SimpleGrantedAuthority(p.getName()))
				.collect(toImmutableList());
	}
}
