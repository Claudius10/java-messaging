package com.example.messaging.common.controller;

import com.example.messaging.common.manager.MessagingManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AppController {

	private final ThreadPoolTaskScheduler workers;

	private final ConfigurableApplicationContext app;

	private final MessagingManager messagingManager;

	@PostMapping("/stop")
	public ResponseEntity<?> stop() {
		if (messagingManager.isProducing()) return ResponseEntity.badRequest().body("Shutdown forbidden: workers are producing. Please stop producer workers first, then try again.");
		if (messagingManager.isConsuming()) return ResponseEntity.badRequest().body("Shutdown forbidden: workers are consuming. Please wait for consumer workers to finish, then try again.");
		log.info("Shutting down in 5 seconds...");
		workers.schedule(this::shutdown, Instant.now().plusSeconds(5));
		return ResponseEntity.ok().build();
	}

	private void shutdown() {
		app.close();
		log.info("Shutdown complete.");
	}
}
