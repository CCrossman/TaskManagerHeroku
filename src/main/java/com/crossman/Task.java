package com.crossman;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public final class Task {
	private String description;

	@JsonCreator
	protected Task() {}

	public Task(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Task task = (Task) o;
		return Objects.equals(getDescription(), task.getDescription());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDescription());
	}

	@Override
	public String toString() {
		return "Task{" +
				"description='" + description + '\'' +
				'}';
	}
}
