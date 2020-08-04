package com.crossman;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.security.core.GrantedAuthority;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;

public final class JWTUtils {
	public static final Algorithm ALGORITHM = Algorithm.HMAC256("secret");
	public static final String ISSUER = "Chris Crossman";

	private JWTUtils() {}

	public static String createToken(String username, Collection<? extends GrantedAuthority> authorities) {
		return createToken(username, authorities, ZonedDateTime.now());
	}

	public static String createToken(String username, Collection<? extends GrantedAuthority> authorities, ZonedDateTime now) {
		return JWT.create()
				.withIssuer(ISSUER)
				.withClaim("username", username)
				.withClaim("user", authorities.contains(GrantedAuthorities.USER))
				.withClaim("admin", authorities.contains(GrantedAuthorities.ADMIN))
				.withExpiresAt(toDate(now.plusMinutes(1)))
				.sign(ALGORITHM);
	}

	private static Date toDate(ZonedDateTime zdt) {
		Date date = new Date();
		date.setTime(zdt.toInstant().toEpochMilli());
		return date;
	}
}
