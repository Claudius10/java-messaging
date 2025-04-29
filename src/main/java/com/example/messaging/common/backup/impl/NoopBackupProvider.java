package com.example.messaging.common.backup.impl;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.model.Dish;
import org.springframework.stereotype.Component;

@Component
public class NoopBackupProvider implements BackupProvider<Dish> {

	@Override
	public void open() {

	}

	@Override
	public void close() {

	}

	@Override
	public boolean hasMoreElements() {
		return false;
	}

	@Override
	public void write(Dish object) {

	}

	@Override
	public Dish read() {
		return null;
	}
}
