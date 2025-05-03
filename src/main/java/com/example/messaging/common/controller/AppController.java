package com.example.messaging.common.controller;

import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.util.APIResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AppController {

	private final TaskScheduler scheduler;

	private final ConfigurableApplicationContext app;

	private final MessagingManager restaurant;

	private final BackupCheck backupCheck;

	@PostMapping("/stop")
	public ResponseEntity<?> stop() {

		boolean producing = restaurant.isProducing();
		boolean consuming = restaurant.isConsuming();

		if (log.isTraceEnabled()) log.trace("Producing -> {}", producing);
		if (log.isTraceEnabled()) log.trace("Consuming -> {}", consuming);

		if (producing || consuming) {
			return ResponseEntity.badRequest().body(APIResponses.SHUTDOWN_WORKING_WARN);
		}

		backupCheck.backupCheck();

		log.info("Shutting down in 5 seconds...");
		scheduler.schedule(this::shutdown, Instant.now().plusSeconds(5));
		return ResponseEntity.ok().body("Shutting down in 5 seconds...");
	}

	private void shutdown() {
		app.close();
		log.info("Shutdown complete.");
	}
}
