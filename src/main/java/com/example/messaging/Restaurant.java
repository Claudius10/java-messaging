package com.example.messaging;

import com.example.messaging.task.async.Manager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Restaurant {

	private final ConfigurableApplicationContext restaurant;

	private final Manager manager;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void open() {
		log.info("Opening restaurant...");
		manager.run();
	}

	@Scheduled(initialDelay = 20, timeUnit = TimeUnit.SECONDS)
	public void close() {
		log.info("Closing restaurant...");
		manager.stop();
		restaurant.close();
		log.info("Restaurant closed");
	}
}