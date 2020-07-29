package com.crossman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.Collections;
import java.util.List;

@Component
public final class DbTaskRepository implements TaskRepository {
	private static final Logger logger = LoggerFactory.getLogger(DbTaskRepository.class);

	private static final String QUERY  = "SELECT taskJson from tasks where username = :usr";
	private static final String INSERT = "INSERT INTO tasks (username, taskJson, timestamp) VALUES (:usr, :task, now()) ON CONFLICT (username) DO UPDATE set taskJson = :task, timestamp = now()";

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private Sql2o sql2o;

	@Override
	public List<Task> getTasksByUsername(String username) {
		logger.debug("getTasksByUsername({})", username);
		try (Connection conn = sql2o.open()) {
			final String taskJson = conn.createQuery(QUERY)
					.addParameter("usr", username)
					.executeScalar(String.class);

			if (taskJson != null && !taskJson.isEmpty()) {
				final List<Task> tasks = objectMapper.readValue(taskJson, new TypeReference<List<Task>>() {
				});
				logger.debug("returning {}", tasks);
				return tasks;
			}
		} catch (JsonProcessingException e) {
			logger.error("There was a problem during getTasksByUsername", e);
		}
		logger.debug("returning empty tasks list");
		return Collections.emptyList();
	}

	@Override
	public void setTasksByUsername(String username, List<Task> tasks) {
		logger.debug("setTasksByUsername({},{})", username, tasks);
		try (Connection conn = sql2o.open()) {
			conn.createQuery(INSERT)
					.addParameter("usr", username)
					.addParameter("task", objectMapper.writeValueAsString(tasks))
					.executeUpdate();
		} catch (JsonProcessingException e) {
			logger.error("There was a problem during setTasksByUsername", e);
		}
	}
}
