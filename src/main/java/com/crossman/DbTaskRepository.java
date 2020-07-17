package com.crossman;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	private static final String CREATE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS public.tasks\n" +
			"(\n" +
			"    username text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
			"    taskJson text COLLATE pg_catalog.\"default\" NOT NULL,\n" +
			"    \"timestamp\" timestamp with time zone NOT NULL,\n" +
			"    CONSTRAINT tasks_username_pkey PRIMARY KEY (username)\n" +
			")";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public List<Task> getTasksByUsername(String username) {
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			stmt.executeUpdate(CREATE_IF_NOT_EXISTS);

			final ResultSet resultSet = stmt.executeQuery("SELECT taskJson from tasks where username = \'" + username + "\'");
			if (resultSet.next()) {
				return objectMapper.readValue(resultSet.getString("taskJson"), new TypeReference<List<Task>>() {
				});
			}
			return Collections.emptyList();
		} catch (SQLException | JsonProcessingException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	@Override
	public void setTasksByUsername(String username, List<Task> tasks) {
		try (Connection connection = dataSource.getConnection()) {
			final Statement stmt = connection.createStatement();
			stmt.executeUpdate(CREATE_IF_NOT_EXISTS);

			String taskJson = objectMapper.writeValueAsString(tasks);

			stmt.executeUpdate("insert into tasks(username, taskJson, timestamp) VALUES (\'" + username + "\', \'" + taskJson + "\', now()) on conflict (username) DO UPDATE set taskJson = \'" + taskJson + "\', timestamp = now();");
		} catch (SQLException | JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
