package com.example.messaging.common.backup.impl;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.model.Dish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class MockBackupProvider implements BackupProvider<Dish> {

	private final AtomicLong count = new AtomicLong(0);

	@Override
	public void open() {
		if (log.isTraceEnabled()) log.trace("Opening backup provider...");
		if (log.isTraceEnabled()) log.trace("Found {} backed up dishes", count.get());
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing backup provider...");
	}

	@Override
	public boolean hasMoreElements() {
		return count.get() != 0;
	}

	@Override
	public void send(Dish dish) {
		checkOpen();
		count.getAndIncrement();
		log.warn("Backed up dish {}", dish.getName());
	}

	@Override
	public Dish read() {
		long countId = count.getAndDecrement();
		return Dish.builder().withId(countId).withCooked(true).withName("Delicious dish " + countId).build();
	}

	@Override
	public void onFailure(Dish dish) {
		log.error("Failed to resend dish '{}'", dish.getName());
	}

	private void checkOpen() {
		// noop
	}
}
