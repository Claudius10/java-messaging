package com.example.messaging.common.backup;

public interface BackupProvider<T> {

	void open();

	void close();

	boolean hasMoreElements();

	void send(T object);

	T read();

	void onFailure(T object);
}
