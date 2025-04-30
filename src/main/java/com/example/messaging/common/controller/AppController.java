package com.example.messaging.common.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.jms.scheduled.JmsTasks;
import com.example.messaging.kafka.scheduled.KafkaTasks;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AppController {

	@Value("${spring.profiles.active:Unknown}")
	private String activeProfile;

	private final TaskScheduler scheduler;

	private final ConfigurableApplicationContext app;

	private final MessagingManager messagingManager;

	private ScheduledFuture<?> backupProcess;

	private final JmsTasks jmsTasks;

	private final KafkaTasks kafkaTasks;

	@PostMapping("/stop")
	public ResponseEntity<?> stop() {

		if (messagingManager.isProducing()) return ResponseEntity.badRequest().body("Shutdown forbidden: workers are producing. Please stop producer workers first, then try again.");
		if (messagingManager.isConsuming()) return ResponseEntity.badRequest().body("Shutdown forbidden: workers are consuming. Please wait for consumer workers to finish, then try again.");
		if (ScheduledFuture.State.RUNNING.equals(backupProcess.state())) return ResponseEntity.badRequest().body("Shutdown forbidden: backup process is running. Please wait, then try again.");

		log.info("Shutting down in 5 seconds...");
		scheduler.schedule(this::shutdown, Instant.now().plusSeconds(5));
		return ResponseEntity.ok().build();
	}

	private void shutdown() {
		app.close();
		log.info("Shutdown complete.");
	}

	@PostConstruct
	private void schedule() {
		switch (activeProfile) {
			case "Jms":
				backupProcess = scheduler.scheduleAtFixedRate(
						jmsTasks::processBackedUpMessages,
						Instant.now().plusSeconds(jmsTasks.getBackupCheckInterval()),
						Duration.ofSeconds(jmsTasks.getBackupCheckInterval())
				);
			case "Kafka":
				backupProcess = scheduler.scheduleAtFixedRate(
						kafkaTasks::processBackedUpMessages,
						Instant.now().plusSeconds(kafkaTasks.getBackupCheckInterval()),
						Duration.ofSeconds(kafkaTasks.getBackupCheckInterval())
				);
			default:
				log.warn("Unknown profile detected");
		}
	}
}
