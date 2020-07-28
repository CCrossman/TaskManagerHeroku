package com.crossman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

@Component
public final class DbAuthenticationProvider implements AuthenticationProvider, AuthenticationSetter {
	private static final Logger logger = LoggerFactory.getLogger(DbAuthenticationProvider.class);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Encoderator encoderator;

	@Override
	public void setAuthorized(Auth auth) {
		logger.debug("setAuthorized({})", auth);
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("INSERT INTO users VALUES (\'" + auth.getUsername() + "\', \'" + encoderator.encode(auth.getPassword()) + "\', now())");
		} catch (SQLException | IOException e) {
			logger.error("There was a problem during setAuthorized", e);
		}
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		logger.debug("authenticate({})", authentication);
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			final String username = authentication.getName();
			final String password = encoderator.encode(String.valueOf(authentication.getCredentials()));
			final ResultSet resultSet = stmt.executeQuery("SELECT timestamp from users where username = \'" + username + "\' and password = \'" + password + "\'");
			if (resultSet.next()) {
				logger.debug("returning a username-password authentication token for {}", username);
				return new UsernamePasswordAuthenticationToken(username, password, Collections.singletonList(GrantedAuthorities.USER));
			}
		} catch (Exception e) {
			logger.error("There was a problem during authenticate", e);
		}
		logger.debug("returning a null token");
		return null;
	}

	@Override
	public boolean supports(Class<?> aClass) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(aClass);
	}
}
