package com.crossman;

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
	private static final String CREATE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS public.users\n" +
			"(\n" +
			"    username text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
			"    password text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
			"    \"timestamp\" timestamp with time zone NOT NULL,\n" +
			"    CONSTRAINT users_pkey PRIMARY KEY (username)\n" +
			")";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Encoderator encoderator;

	@Override
	public void setAuthorized(Auth auth) {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(CREATE_IF_NOT_EXISTS);
			stmt.executeUpdate("INSERT INTO users VALUES (\'" + auth.getUsername() + "\', \'" + encoderator.encode(auth.getPassword()) + "\', now())");
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			stmt.executeUpdate(CREATE_IF_NOT_EXISTS);

			final String username = authentication.getName();
			final String password = encoderator.encode(String.valueOf(authentication.getCredentials()));
			final ResultSet resultSet = stmt.executeQuery("SELECT timestamp from users where username = \'" + username + "\' and password = \'" + password + "\'");
			if (resultSet.next()) {
				return new UsernamePasswordAuthenticationToken(username, password, Collections.singletonList(GrantedAuthorities.USER));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean supports(Class<?> aClass) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(aClass);
	}
}
