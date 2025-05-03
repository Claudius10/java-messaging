package com.example.messaging.common.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.scheduled.BackupCheck;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AppController {

	private final TaskScheduler scheduler;

	private final ConfigurableApplicationContext app;

	private final MessagingManager messagingManager;

	private final BackupCheck backupCheck;

	private ScheduledFuture<?> backupProcess;

	@PostConstruct
	private void scheduleBackUpCheck() {
		backupProcess = backupCheck.scheduleBackupCheck(scheduler);
	}

	@PostMapping("/stop")
	public ResponseEntity<?> stop() {

		if (messagingManager.isProducing()) {
			return ResponseEntity.badRequest().body("Shutdown forbidden: workers are producing. Please stop producer workers first, then try again.");
		}

		if (messagingManager.isConsuming()) {
			return ResponseEntity.badRequest().body("Shutdown forbidden: workers are consuming. Please wait for consumer workers to finish, then try again.");
		}

		if (ScheduledFuture.State.RUNNING.equals(backupProcess.state())) {
			return ResponseEntity.badRequest().body("Shutdown forbidden: backup process is running. Please wait, then try again.");
		}

		log.info("Shutting down in 5 seconds...");
		scheduler.schedule(this::shutdown, Instant.now().plusSeconds(5));
		return ResponseEntity.ok().build();
	}

	private void shutdown() {
		app.close();
		log.info("Shutdown complete.");
	}
}
