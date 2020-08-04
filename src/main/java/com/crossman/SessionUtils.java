package com.crossman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Collections;

public final class SessionUtils {
	private static final Logger logger = LoggerFactory.getLogger(SessionUtils.class);

	private SessionUtils() {}

	public static SecurityContext getSecurityContext(HttpSession session) {
		if (session == null) {
			return null;
		}
		final Object o = session.getAttribute("SPRING_SECURITY_CONTEXT");
		if (o == null) {
			return null;
		} else if (o instanceof SecurityContext) {
			return ((SecurityContext) o);
		} else {
			logger.error("Unexpected security context type '{}'", o.getClass());
			return null;
		}
	}

	public static String getUsernameFromSession(HttpSession session) {
		return getUsernameFromSecurityContext(getSecurityContext(session));
	}

	public static String getUsernameFromSecurityContext(SecurityContext sc) {
		if (sc == null) {
			return null;
		}
		Authentication authentication = sc.getAuthentication();
		if (authentication == null) {
			return null;
		}
		return authentication.getName();
	}

	public static Collection<? extends GrantedAuthority> getAuthoritiesFromSecurityContext(SecurityContext sc) {
		if (sc == null) {
			return Collections.emptyList();
		}
		Authentication authentication = sc.getAuthentication();
		if (authentication == null) {
			return Collections.emptyList();
		}
		return authentication.getAuthorities();
	}
}
