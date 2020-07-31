package com.crossman;

public final class Permissions {
	public final String username;
	public final String message;
	public final boolean user;
	public final boolean admin;

	public Permissions(String username, String message, boolean user, boolean admin) {
		this.username = username;
		this.message = message;
		this.user = user;
		this.admin = admin;
	}
}
