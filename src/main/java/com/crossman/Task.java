package com.crossman;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public final class Task {
	private String description;
	private boolean checked;

	@JsonCreator
	protected Task() {}

	public Task(String description, boolean checked) {
		this.description = description;
		this.checked = checked;
	}

	public String getDescription() {
		return description;
	}

	public boolean isChecked() {
		return checked;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Task task = (Task) o;
		return isChecked() == task.isChecked() &&
				Objects.equals(getDescription(), task.getDescription());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDescription(), isChecked());
	}

	@Override
	public String toString() {
		return "Task{" +
				"description='" + description + '\'' +
				", checked=" + checked +
				'}';
	}
}
