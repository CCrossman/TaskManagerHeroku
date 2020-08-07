package com.crossman;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

import static com.crossman.Permissions.checkAdmin;
import static com.crossman.Permissions.checkUser;
import static com.crossman.SessionUtils.*;

@RestController
@RequestMapping("/api")
public class ApiController {
	private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private Promoter promoter;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserDeleter userDeleter;

	@Autowired
	private UserLister userLister;

	@RequestMapping(value = "/jwt/extend", method = RequestMethod.POST)
	public String jwtExtend(HttpSession session, @RequestBody String jwt) {
		final SecurityContext sc = getSecurityContext(session);
		final String username = getUsernameFromSecurityContext(sc);

		logger.debug("jwtExtend({},{})", username, jwt);

		final DecodedJWT decodedJWT = jwtUtils.getVerifierForExtendableTokens().verify(jwt);
		final String tokenUsername = decodedJWT.getClaim("username").asString();
		if (!username.equals(tokenUsername)) {
			logger.debug("comparing {} to {}", username, tokenUsername);
			throw new IllegalArgumentException("token must be owned by the session user");
		}
		// if valid, create a new token
		return createToken(sc, username);
	}

	@RequestMapping(value = "/jwt", method = RequestMethod.POST, headers = {"Authorization"})
	public Permissions jwtAuth(@RequestHeader("Authorization") Permissions permissions) {
		logger.debug("{}", permissions);
		return permissions;
	}

	@RequestMapping(value = "/jwt", method = RequestMethod.GET)
	public String jwtCalculate(HttpSession session) {
		final SecurityContext sc = getSecurityContext(session);
		final String username = getUsernameFromSecurityContext(sc);
		logger.debug("jwtCalculate({})", username);
		return createToken(sc, username);
	}

	@RequestMapping(value = "/promote/{username}", method = RequestMethod.POST, headers = {"Authorization"})
	public String promote(@RequestHeader("Authorization") Permissions permissions, @PathVariable("username") String promoteeUsername) throws InsufficientPermissionsException {
		logger.debug("promote({},{})", permissions, promoteeUsername);
		checkAdmin(permissions);

		logger.debug("{} has promoted {} successfully.", permissions.username, promoteeUsername);
		promoter.promote(promoteeUsername);
		return permissions.username + " has promoted " + promoteeUsername + " successfully";

	}

	@RequestMapping(value = "/save", method = RequestMethod.POST, headers = {"Authorization"})
	public String save(@RequestHeader("Authorization") Permissions permissions, @RequestBody List<Task> tasks) throws InsufficientPermissionsException {
		logger.debug("save({},{})", permissions, tasks);
		checkUser(permissions);

		logger.debug("{} task list updated.", permissions.username);
		taskRepository.setTasksByUsername(permissions.username, tasks);
		return permissions.username + " task list updated.";
	}

	@RequestMapping(value = "/users", method = RequestMethod.GET, headers = {"Authorization"})
	public List<String> getUsers(@RequestHeader("Authorization") Permissions permissions) throws InsufficientPermissionsException {
		logger.debug("getUsers({})", permissions);
		checkAdmin(permissions);

		logger.debug("{} retrieved user list", permissions.username);
		return userLister.getUsers();
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.GET, headers = {"Authorization"})
	public UserSummary getUser(@RequestHeader("Authorization") Permissions permissions, @PathVariable("username") String targetUsername) throws InsufficientPermissionsException {
		logger.debug("getUser({},{})", permissions, targetUsername);
		checkAdmin(permissions);

		logger.debug("{} retrieved user summary for {}", permissions.username, targetUsername);
		return new UserSummary(targetUsername, taskRepository.getTasksByUsername(targetUsername));
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.DELETE, headers = {"Authorization"})
	public UserSummary deleteUser(@RequestHeader("Authorization") Permissions permissions, @PathVariable("username") String targetUsername) throws InsufficientPermissionsException {
		logger.debug("deleteUser({},{})", permissions, targetUsername);
		checkAdmin(permissions);

		logger.debug("{} deleted user {}", permissions.username, targetUsername);
		List<Task> tasks = taskRepository.getTasksByUsername(targetUsername);
		userDeleter.deleteUser(targetUsername);
		return new UserSummary(targetUsername, tasks);
	}

	private String createToken(SecurityContext sc, String username) {
		return jwtUtils.createToken(username, getAuthoritiesFromSecurityContext(sc));
	}
}
