package com.crossman;

import java.util.List;

public interface TaskRepository {
	public List<Task> getTasksByUsername(String username);
	public void setTasksByUsername(String username, List<Task> tasks);
}
