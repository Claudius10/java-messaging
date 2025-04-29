package com.example.messaging.common.backup;

public interface BackupProvider<T> {

	void open();

	void close();

	boolean hasMoreElements();

	void write(T object);

	T read();
}
