package com.crossman;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public final class InsufficientPermissionsException extends Exception {
	public InsufficientPermissionsException() {
	}

	public InsufficientPermissionsException(String message) {
		super(message);
	}

	public InsufficientPermissionsException(String message, Throwable cause) {
		super(message, cause);
	}

	public InsufficientPermissionsException(Throwable cause) {
		super(cause);
	}

	public InsufficientPermissionsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
