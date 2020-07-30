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
import org.sql2o.ResultSetHandler;
import org.sql2o.Sql2o;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public final class DbAuthenticationProvider implements AuthenticationProvider, AuthenticationSetter, Promoter {
	private static final Logger logger = LoggerFactory.getLogger(DbAuthenticationProvider.class);
	private static final String USER_INSERT = "INSERT INTO users (username, password, timestamp) VALUES (:usr, :pwd, now())";
	private static final String USER_QUERY  = "SELECT COUNT(*) from users where username = :usr and password = :pwd";
	private static final String ROLE_INSERT = "INSERT INTO roles (username, role) VALUES (:usr, 'USER')";
	private static final String ROLE_INSERT_ADMIN = "INSERT INTO roles (username, role) VALUES (:usr, 'ADMIN')";
	private static final String ROLE_QUERY  = "SELECT role FROM roles where username = :usr";

	private static final ResultSetHandler<GrantedAuthorities> ROLE_PARSER = new ResultSetHandler<GrantedAuthorities>() {
		@Override
		public GrantedAuthorities handle(ResultSet resultSet) throws SQLException {
			return GrantedAuthorities.valueOf(resultSet.getString("role"));
		}
	};

	@Autowired
	private Encoderator encoderator;

	@Autowired
	private Sql2o sql2o;

	@Override
	public void promote(String username) {
		logger.debug("promote({})", username);
		try (Connection conn = sql2o.open()) {
			conn.createQuery(ROLE_INSERT_ADMIN)
					.addParameter("usr", username)
					.executeUpdate();
		} catch (Exception e) {
			logger.error("There was a problem during promote", e);
		}
	}

	@Override
	public void setAuthorized(Auth auth) {
		logger.debug("setAuthorized({})", auth);
		try (Connection conn = sql2o.open()) {
			conn.createQuery(USER_INSERT)
					.addParameter("usr", auth.getUsername())
					.addParameter("pwd", encoderator.encode(auth.getPassword()))
					.executeUpdate();

			conn.createQuery(ROLE_INSERT)
					.addParameter("usr", auth.getUsername())
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
			final int count = conn.createQuery(USER_QUERY)
					.addParameter("usr", username)
					.addParameter("pwd", password)
					.executeScalar(Integer.class);
			if (count > 0) {
				final List<GrantedAuthorities> roles = conn.createQuery(ROLE_QUERY)
						.addParameter("usr", username)
						.executeAndFetch(ROLE_PARSER);

				logger.debug("returning a username-password authentication token for {}", username);
				return new UsernamePasswordAuthenticationToken(username, password, roles);
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
