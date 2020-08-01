package com.crossman;

import java.util.List;

public final class UserSummary {
	private final String username;
	private final List<Task> tasks;

	public UserSummary(String username, List<Task> tasks) {
		this.username = username;
		this.tasks = tasks;
	}

	public String getUsername() {
		return username;
	}

	public List<Task> getTasks() {
		return tasks;
	}
}
