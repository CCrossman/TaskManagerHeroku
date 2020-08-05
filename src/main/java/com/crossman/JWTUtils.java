package com.crossman;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;

@Component
public final class JWTUtils {

	@Autowired
	private Algorithm algorithm;

	@Autowired
	@Qualifier("issuer")
	private String issuer;

	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public String getIssuer() {
		return issuer;
	}

	public String createToken(String username, Collection<? extends GrantedAuthority> authorities) {
		return createToken(username, authorities, ZonedDateTime.now());
	}

	public String createToken(String username, Collection<? extends GrantedAuthority> authorities, ZonedDateTime now) {
		return JWT.create()
				.withIssuer(issuer)
				.withClaim("username", username)
				.withClaim("user", authorities.contains(GrantedAuthorities.USER))
				.withClaim("admin", authorities.contains(GrantedAuthorities.ADMIN))
				.withExpiresAt(toDate(now.plusMinutes(1)))
				.sign(algorithm);
	}

	private static Date toDate(ZonedDateTime zdt) {
		Date date = new Date();
		date.setTime(zdt.toInstant().toEpochMilli());
		return date;
	}
}
