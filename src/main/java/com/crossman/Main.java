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

import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

@Controller
@SpringBootApplication
public class Main {

	@Autowired
	private Authority authority;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

	@RequestMapping(value = "/signup", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
	String signup(@ModelAttribute Auth auth) {
		if (auth == null || auth.getUsername() == null || auth.getPassword() == null) {
			return "signup";
		}
		authority.setAuthorized(auth);
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
		if (authority.isAuthorized(auth)) {
			session.setAttribute("username", auth.getUsername());
			return "redirect:/todo";
		}
		return "redirect:/login";
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

}
