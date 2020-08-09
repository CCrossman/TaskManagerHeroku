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

package com.crossman;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.sql2o.Sql2o;

import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Controller
@SpringBootApplication
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	@Autowired
	private ApiController apiController;

	@Autowired
	private AuthenticationSetter authenticationSetter;

	@Autowired
	private Encoderator encoderator;

	@Autowired
	private TaskRepository taskRepository;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

	@RequestMapping(value = "/signup", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
	String signup(@ModelAttribute Auth auth, Model model, RedirectAttributes redirectAttributes) {
		logger.debug("signup({})", auth);
		if (auth == null || auth.getUsername() == null || auth.getPassword() == null) {
			logger.debug("rendering signup page");
			return "signup";
		}
		try {
			logger.debug("marking {} as authorized user", auth.getUsername());
			authenticationSetter.setAuthorized(auth);

			logger.trace("logging in as new user");
			final String password = encoderator.encode(auth.getPassword());
			final List<GrantedAuthorities> authorities = Collections.singletonList(GrantedAuthorities.USER);
			final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(auth.getUsername(), password, authorities);
			SecurityContextHolder.getContext().setAuthentication(token);

			logger.trace("redirect attributes set");
			redirectAttributes.addFlashAttribute("infos", Collections.singletonList("Signup was a success!"));

			logger.debug("redirecting to todo page");
			return "redirect:/todo";
		} catch (Exception e) {
			logger.error("There was a problem during signup.", e);
			model.addAttribute("errors", Collections.singletonList("There was a problem during signup."));

			logger.debug("rendering signup page");
			return "signup";
		}
	}

	@RequestMapping("/todo")
	String todo(HttpSession session, Model model) {
		final String username = SessionUtils.getUsernameFromSession(session);
		logger.debug("todo({})", username);
		if (username == null) {
			logger.debug("redirecting to login page");
			return "redirect:/login";
		}
		if (!model.containsAttribute("tasks")) {
			final List<Task> tasks = taskRepository.getTasksByUsername(username);
			logger.trace("tasks = {}", tasks);
			model.addAttribute("tasks", tasks);
		}
		logger.debug("rendering todo page");
		model.addAttribute("jwt", apiController.jwtCalculate(session));
		return "todo";
	}

	@RequestMapping("/admin")
	String admin(HttpSession session, Model model) {
		logger.debug("rendering admin page");
		final SecurityContext sessionContext = SessionUtils.getSecurityContext(session);
		final String username = SessionUtils.getUsernameFromSession(session);
		if (username == null || !SessionUtils.getAuthoritiesFromSecurityContext(sessionContext).contains(GrantedAuthorities.ADMIN)) {
			logger.debug("rediecting to login page");
			return "redirect:/login";
		}
		model.addAttribute("jwt", apiController.jwtCalculate(session));
		return "admin";
	}

	@Bean
	public DataSource dataSource() {
		String db  = System.getenv("db-url");
		String usr = System.getenv("db-username");
		String pwd = System.getenv("db-password");

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(db + "?user=" + usr + "&password=" + pwd);
		config.setDriverClassName("org.postgresql.Driver");
		return new HikariDataSource(config);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public SpringLiquibase liquibase() {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setChangeLog("classpath:dbchangelog.xml");
		liquibase.setDataSource(dataSource());
		return liquibase;
	}

	@Bean
	public Sql2o sql2o() {
		return new Sql2o(dataSource());
	}

	@Bean
	@Qualifier("issuer")
	public String issuer() {
		return "Chris Crossman";
	}

	@Bean
	public Algorithm algorithm() {
		final String secret = System.getenv("jwt-secret");
		if (secret == null) {
			throw new NullPointerException("JWT secret cannot be null");
		}
		return Algorithm.HMAC256(secret);
	}
}
