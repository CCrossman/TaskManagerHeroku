package com.crossman;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

@Component
public final class DbAuthority implements Authority {
	private static final String CREATE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS public.users\n" +
			"(\n" +
			"    username text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
			"    password text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
			"    \"timestamp\" timestamp with time zone NOT NULL,\n" +
			"    CONSTRAINT users_pkey PRIMARY KEY (username)\n" +
			")";

	@Autowired
	private DataSource dataSource;

	public boolean isAuthorized(Auth auth) {
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			stmt.executeUpdate(CREATE_IF_NOT_EXISTS);

			final String pwd = encodePassword(auth.getPassword());
			final ResultSet resultSet = stmt.executeQuery("SELECT username from users where password = \'" + pwd + "\'");
			while (resultSet.next()) {
				if (auth.getUsername().equals(resultSet.getString("username"))) {
					return true;
				}
			}
			return false;
		} catch (IOException | SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setAuthorized(Auth auth) throws IOException {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(CREATE_IF_NOT_EXISTS);
			stmt.executeUpdate("INSERT INTO users VALUES (\'" + auth.getUsername() + "\', \'" + encodePassword(auth.getPassword()) + "\', now())");
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	// https://howtodoinjava.com/security/aes-256-encryption-decryption/
	private static String encodePassword(String password) throws IOException {
		try {
			String secretKey = System.getenv("enc-secret");
			String salt = System.getenv("enc-salt");

			byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
			return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException e) {
			throw new IOException(e);
		}
	}
}
