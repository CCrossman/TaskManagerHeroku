package com.crossman;

import java.util.Objects;

public final class Auth {
	private String username;
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Auth auth = (Auth) o;
		return Objects.equals(getUsername(), auth.getUsername()) &&
				Objects.equals(getPassword(), auth.getPassword());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUsername(), getPassword());
	}

	@Override
	public String toString() {
		return "Auth{" +
				"username='" + username + '\'' +
				", password='<censored>'" +
				'}';
	}
}
