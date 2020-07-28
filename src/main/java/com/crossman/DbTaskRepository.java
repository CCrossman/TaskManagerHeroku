package com.crossman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

@Component
public final class DbTaskRepository implements TaskRepository {
	private static final Logger logger = LoggerFactory.getLogger(DbTaskRepository.class);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public List<Task> getTasksByUsername(String username) {
		logger.debug("getTasksByUsername({})", username);
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			final ResultSet resultSet = stmt.executeQuery("SELECT taskJson from tasks where username = \'" + username + "\'");
			if (resultSet.next()) {
				final List<Task> tasks = objectMapper.readValue(resultSet.getString("taskJson"), new TypeReference<List<Task>>() {
				});
				logger.debug("returning {}", tasks);
				return tasks;
			}
		} catch (SQLException | JsonProcessingException e) {
			logger.error("There was a problem during getTasksByUsername", e);
		}
		logger.debug("returning empty tasks list");
		return Collections.emptyList();
	}

	@Override
	public void setTasksByUsername(String username, List<Task> tasks) {
		logger.debug("setTasksByUsername({},{})", username, tasks);
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			final String taskJson = objectMapper.writeValueAsString(tasks);
			stmt.executeUpdate("insert into tasks(username, taskJson, timestamp) VALUES (\'" + username + "\', \'" + taskJson + "\', now()) on conflict (username) DO UPDATE set taskJson = \'" + taskJson + "\', timestamp = now();");
		} catch (SQLException | JsonProcessingException e) {
			logger.error("There was a problem during setTasksByUsername", e);
		}
	}
}
