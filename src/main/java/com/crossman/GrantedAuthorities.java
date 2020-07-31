package com.crossman;

import org.springframework.security.core.GrantedAuthority;

public enum GrantedAuthorities implements GrantedAuthority {
	ADMIN,
	USER;

	@Override
	public String getAuthority() {
		return name();
	}
}