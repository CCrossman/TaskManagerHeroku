package com.crossman;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
	private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

	private static final Algorithm algorithm = Algorithm.HMAC256("secret");
	private static final String ISSUER = "Chris Crossman";

	@Autowired
	private Promoter promoter;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserDeleter userDeleter;

	@Autowired
	private UserLister userLister;

	@RequestMapping(value = "/jwt", method = RequestMethod.POST)
	public Permissions jwtAuth(@RequestHeader("Authorization") String bearer) {
		logger.debug(bearer);

		final DecodedJWT decodedJWT = JWT.require(algorithm)
				.withIssuer(ISSUER)
				.build()
				.verify(bearer.substring("bearer ".length()));

		final String username = decodedJWT.getClaim("username").asString();
		final boolean isUser = decodedJWT.getClaim("user").asBoolean();
		final boolean isAdmin = decodedJWT.getClaim("admin").asBoolean();

		Date now = new Date();
		if (now.before(decodedJWT.getNotBefore())) {
			return new Permissions(username, "not before now", false, false);
		}
		if (decodedJWT.getExpiresAt().before(now)) {
			return new Permissions(username, "expired token", false, false);
		}
		return new Permissions(username, "ok", isUser, isAdmin);
	}

	@RequestMapping(value = "/jwt", method = RequestMethod.GET)
	public String jwtCalculate(HttpSession session) {
		final SecurityContext sc = getSecurityContext(session);
		final String username = getUsernameFromSecurityContext(sc);
		if (username == null) {
			throw new IllegalStateException("username cannot be blank");
		}
		final Collection<? extends GrantedAuthority> authorities = getAuthoritiesFromSecurityContext(sc);
		final ZonedDateTime now = ZonedDateTime.now();
		return JWT.create()
				.withIssuer(ISSUER)
				.withClaim("username", username)
				.withClaim("user", authorities.contains(GrantedAuthorities.USER))
				.withClaim("admin", authorities.contains(GrantedAuthorities.ADMIN))
				.withExpiresAt(toDate(now.plusMinutes(15)))
				.withNotBefore(toDate(now))
				.sign(algorithm);
	}

	@RequestMapping(value = "/promote/{username}", method = RequestMethod.POST)
	ResponseEntity<String> promote(@RequestHeader("Authorization") String bearer, @PathVariable("username") String promoteeUsername) {
		logger.debug("promote({},{})", bearer, promoteeUsername);
		final Permissions permissions = jwtAuth(bearer);
		if (permissions.user && permissions.admin) {
			logger.debug("{} has promoted {} successfully.", permissions.username, promoteeUsername);
			promoter.promote(promoteeUsername);
			return new ResponseEntity<>(permissions.username + " has promoted " + promoteeUsername + " successfully", HttpStatus.OK);
		}
		logger.debug("{} must be an ADMIN to promote someone.", permissions.username);
		return new ResponseEntity<>(permissions.username + " must be an ADMIN to promote someone.", HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/save", method = RequestMethod.POST)
	ResponseEntity<String> save(@RequestHeader("Authorization") String bearer, @RequestBody List<Task> tasks) {
		logger.debug("save({},{})", bearer, tasks);
		final Permissions permissions = jwtAuth(bearer);
		if (permissions.user) {
			logger.debug("{} task list updated.", permissions.username);
			taskRepository.setTasksByUsername(permissions.username, tasks);
			return new ResponseEntity<>(permissions.username + " task list updated.", HttpStatus.OK);
		}
		return new ResponseEntity<>("invalid permissions", HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/users", method = RequestMethod.GET)
	ResponseEntity<List<String>> getUsers(@RequestHeader("Authorization") String bearer) {
		logger.debug("getUsers({})", bearer);
		final Permissions permissions = jwtAuth(bearer);
		if (permissions.user && permissions.admin) {
			logger.debug("{} retrieved user list", permissions.username);
			return new ResponseEntity<>(userLister.getUsers(), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.GET)
	ResponseEntity<UserSummary> getUser(@RequestHeader("Authorization") String bearer, @PathVariable("username") String targetUsername) {
		logger.debug("getUser({},{})", bearer, targetUsername);
		final Permissions permissions = jwtAuth(bearer);
		if (permissions.user && permissions.admin) {
			logger.debug("{} retrieved user summary for {}", permissions.username, targetUsername);
			return new ResponseEntity<>(new UserSummary(targetUsername, taskRepository.getTasksByUsername(targetUsername)), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.DELETE)
	ResponseEntity<UserSummary> deleteUser(@RequestHeader("Authorization") String bearer, @PathVariable("username") String targetUsername) {
		logger.debug("deleteUser({},{})", bearer, targetUsername);
		final Permissions permissions = jwtAuth(bearer);
		if (permissions.user && permissions.admin) {
			logger.debug("{} deleted user {}", permissions.username, targetUsername);
			List<Task> tasks = taskRepository.getTasksByUsername(targetUsername);
			userDeleter.deleteUser(targetUsername);
			return new ResponseEntity<>(new UserSummary(targetUsername, tasks), HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.FORBIDDEN);
	}

	private static Date toDate(ZonedDateTime zdt) {
		Date date = new Date();
		date.setTime(zdt.toInstant().toEpochMilli());
		return date;
	}

	private static SecurityContext getSecurityContext(HttpSession session) {
		if (session == null) {
			return null;
		}
		final Object o = session.getAttribute("SPRING_SECURITY_CONTEXT");
		if (o == null) {
			return null;
		} else if (o instanceof SecurityContext) {
			return ((SecurityContext) o);
		} else {
			logger.error("Unexpected security context type '{}'", o.getClass());
			return null;
		}
	}

	private static String getUsernameFromSecurityContext(SecurityContext sc) {
		if (sc == null) {
			return null;
		}
		Authentication authentication = sc.getAuthentication();
		if (authentication == null) {
			return null;
		}
		return authentication.getName();
	}

	private static Collection<? extends GrantedAuthority> getAuthoritiesFromSecurityContext(SecurityContext sc) {
		if (sc == null) {
			return Collections.emptyList();
		}
		Authentication authentication = sc.getAuthentication();
		if (authentication == null) {
			return Collections.emptyList();
		}
		return authentication.getAuthorities();
	}

}
