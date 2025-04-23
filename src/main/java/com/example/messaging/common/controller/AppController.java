package com.example.messaging.common.controller;

import com.example.messaging.common.restaurant.Restaurant;
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

	private final ConfigurableApplicationContext restaurantControls;

	private final Restaurant restaurant;

	@PostMapping("/close")
	public ResponseEntity<?> close() {
		if (restaurant.isCooking()) return ResponseEntity.badRequest().body("Close forbidden: restaurant is producing. Please stop chefs first, then try again.");
		if (restaurant.isServing()) return ResponseEntity.badRequest().body("Close forbidden: restaurant is consuming. Please wait for servers to finish, then try again.");
		log.info("Closing restaurant in 5 seconds...");
		workers.schedule(this::shutdown, Instant.now().plusSeconds(5));
		return ResponseEntity.ok().build();
	}

	private void shutdown() {
		restaurantControls.close();
		log.info("Restaurant closed.");
	}
}
