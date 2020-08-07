package com.crossman;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PermissionsConverter implements Converter<String,Permissions> {
	private static final int PREFIX = "bearer ".length();

	@Autowired
	private JWTUtils jwtUtils;

	@Override
	public Permissions convert(String bearer) {
		final DecodedJWT decodedJWT = jwtUtils.getVerifier().verify(bearer.substring(PREFIX));
		final String username = decodedJWT.getClaim("username").asString();
		final boolean isUser = decodedJWT.getClaim("user").asBoolean();
		final boolean isAdmin = decodedJWT.getClaim("admin").asBoolean();
		final Date now = new Date();
		if (decodedJWT.getExpiresAt().before(now)) {
			return new Permissions(username, "expired token", false, false);
		}
		return new Permissions(username, "ok", isUser, isAdmin);
	}
}
