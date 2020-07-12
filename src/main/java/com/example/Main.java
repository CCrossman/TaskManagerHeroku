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
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.base64.Base64Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
@SpringBootApplication
public class Main {
	public static final String SECRET_KEY = "secret key";
	public static final String SALT = "salt";

	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Autowired
	private DataSource dataSource;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

//  @RequestMapping("/")
//  String index() {
//    return "index";
//  }

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
		System.err.println("session: " + session.getId());
		System.err.println("username: " + session.getAttribute("username"));
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

	private final Map<String,String> authorizations = new HashMap<>();

	private boolean isAuthorized(Auth auth) {
		try {
			return encodePassword(auth.getPassword()).equals(authorizations.get(auth.getUsername()));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void setAuthorized(Auth auth) {
		try {
			System.err.println("setAuthorized(" + auth + ")");
			authorizations.put(auth.getUsername(), encodePassword(auth.getPassword()));
		} catch (IOException e) {
			e.printStackTrace();
		}

//		try (Connection connection = dataSource.getConnection()) {
//			Statement stmt = connection.createStatement();
//			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (id, encodedpwd, timestamp)");
//			stmt.executeUpdate("INSERT INTO users VALUES (" + auth.getUsername() + ", " + encodePassword(auth.getPassword()) + ", now())");
//			System.err.println("done!");
//		} catch (SQLException | IOException e) {
//			e.printStackTrace();
//		}
	}

	// https://howtodoinjava.com/security/aes-256-encryption-decryption/
	private static String encodePassword(String password) throws IOException {
		try {
			byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
			IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
			return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes(StandardCharsets.UTF_8)));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException e) {
			throw new IOException(e);
		}
	}

//  @RequestMapping("/db")
//  String db(Map<String, Object> model) {
//    try (Connection connection = dataSource.getConnection()) {
//      Statement stmt = connection.createStatement();
//      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
//      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
//      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");
//
//      ArrayList<String> output = new ArrayList<String>();
//      while (rs.next()) {
//        output.add("Read from DB: " + rs.getTimestamp("tick"));
//      }
//
//      model.put("records", output);
//      return "db";
//    } catch (Exception e) {
//      model.put("message", e.getMessage());
//      return "error";
//    }
//  }

	@Bean
	public DataSource dataSource() throws SQLException {
		if (dbUrl == null || dbUrl.isEmpty()) {
			return new HikariDataSource();
		} else {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(dbUrl);
			return new HikariDataSource(config);
		}
	}

}
