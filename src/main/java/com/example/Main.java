/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpSession;
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

@Controller
@SpringBootApplication
public class Main {

	@Autowired
	private DataSource dataSource;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

	@RequestMapping(value = "/signup", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
	String signup(@ModelAttribute Auth auth) {
		if (auth == null || auth.getUsername() == null || auth.getPassword() == null) {
			return "signup";
		}
		setAuthorized(auth);
		return "redirect:/login";
	}

	@RequestMapping("/todo")
	String todo(HttpSession session, Model model) {
		if (session == null || session.getAttribute("username") == null) {
			model.addAttribute("auth", new Auth());
			return "redirect:/login";
		}
		return "todo";
	}

	@RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
	String login(@ModelAttribute Auth auth, HttpSession session) {
		if (auth == null || auth.getUsername() == null || auth.getPassword() == null) {
			return "login";
		}
		if (isAuthorized(auth)) {
			session.setAttribute("username", auth.getUsername());
			return "redirect:/todo";
		}
		return "redirect:/login";
	}

	private boolean isAuthorized(Auth auth) {
		try (Connection connection = dataSource.getConnection()) {
			final String pwd = encodePassword(auth.getPassword());
			final ResultSet resultSet = connection.createStatement().executeQuery("SELECT username from users where password = \'" + pwd + "\'");
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

	private void setAuthorized(Auth auth) {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			final String sql = "INSERT INTO users VALUES (\'" + auth.getUsername() + "\', \'" + encodePassword(auth.getPassword()) + "\', now())";
			stmt.executeUpdate(sql);
		} catch (SQLException | IOException e) {
			e.printStackTrace();
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

	@Bean
	public DataSource dataSource() throws SQLException {
		String usr = System.getenv("db-username");
		String pwd = System.getenv("db-password");

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:postgresql://localhost:5432/test?user=" + usr + "&password=" + pwd);
		config.setDriverClassName("org.postgresql.Driver");
		return new HikariDataSource(config);
	}

}
