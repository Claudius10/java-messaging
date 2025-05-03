package com.example.messaging.common.backup.impl;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.model.Dish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockBackupProvider implements BackupProvider<Dish> {

	private long count = 0;

	@Override
	public void open() {
		if (log.isTraceEnabled()) log.trace("Opening backup provider...");
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing backup provider...");
	}

	@Override
	public boolean hasMoreElements() {
		return count != 0;
	}

	@Override
	public void write(Dish dish) {
		checkOpen();
		if (log.isTraceEnabled()) log.trace("Backing up dish {}", dish.getName());
		count++;
	}

	@Override
	public Dish read() {
		Dish dish = Dish.builder().withId(count).withCooked(true).withName("Delicious dish " + count).build();
		count--;
		return dish;
	}

	@Override
	public void onFailure(Dish dish) {
		if (log.isTraceEnabled()) log.trace("Failed to resend dish {}", dish.getName());
	}

	private void checkOpen() {
		// noop
	}
}
