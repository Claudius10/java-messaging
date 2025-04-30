package com.example.messaging.common.exception.backup;

public class BackupProcessException extends RuntimeException {

	public BackupProcessException(String message) {
		super(message);
	}

	public BackupProcessException() {
		super();
	}
}
