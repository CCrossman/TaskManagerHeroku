package com.crossman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.io.IOException;
import java.util.Collections;

@Component
public final class DbAuthenticationProvider implements AuthenticationProvider, AuthenticationSetter {
	private static final Logger logger = LoggerFactory.getLogger(DbAuthenticationProvider.class);
	private static final String INSERT = "INSERT INTO users (username, password, timestamp) VALUES (:usr, :pwd, now())";
	private static final String QUERY  = "SELECT COUNT(*) from users where username = :usr and password = :pwd";

	@Autowired
	private Encoderator encoderator;

	@Autowired
	private Sql2o sql2o;

	@Override
	public void setAuthorized(Auth auth) {
		logger.debug("setAuthorized({})", auth);
		try (Connection conn = sql2o.open()) {
			conn.createQuery(INSERT)
					.addParameter("usr", auth.getUsername())
					.addParameter("pwd", encoderator.encode(auth.getPassword()))
					.executeUpdate();
		} catch (Exception e) {
			logger.error("There was a problem during setAuthorized", e);
		}
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		logger.debug("authenticate({})", authentication);
		try (Connection conn = sql2o.open()) {
			final String username = authentication.getName();
			final String password = encoderator.encode(String.valueOf(authentication.getCredentials()));
			final int count = conn.createQuery(QUERY)
					.addParameter("usr", username)
					.addParameter("pwd", password)
					.executeScalar(Integer.class);
			if (count > 0) {
				logger.debug("returning a username-password authentication token for {}", username);
				return new UsernamePasswordAuthenticationToken(username, password, Collections.singletonList(GrantedAuthorities.USER));
			}
		} catch (IOException e) {
			logger.error("Problem during authentication step.", e);
			throw new InternalAuthenticationServiceException("Problem during authentication step.", e);
		}
		logger.debug("returning a null token");
		return null;
	}

	@Override
	public boolean supports(Class<?> aClass) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(aClass);
	}
}
