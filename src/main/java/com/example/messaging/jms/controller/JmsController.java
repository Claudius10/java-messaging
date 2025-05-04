package com.example.messaging.jms.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.jms.consumer.JmsConsumerManager;
import com.example.messaging.jms.consumer.JmsConsumerMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("Jms")
@RestController
@RequestMapping("/jms")
@RequiredArgsConstructor
@Slf4j
public class JmsController {

	private final MessagingManager myJmsRestaurant;

	private final JmsConsumerManager consumerOperations;

	private final JmsConsumerMetrics metrics;

	@PostMapping("/producer/start")
	public ResponseEntity<?> startProducer() {
		log.info("Opening JMS restaurant...");
		myJmsRestaurant.open();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/producer/stop")
	public ResponseEntity<?> stopProducer() {
		try {
			log.info("Closing JMS restaurant...");
			myJmsRestaurant.close();
			return ResponseEntity.ok().build();
		} catch (InterruptedException e) {
			log.error("Interrupted with closing JMS Restaurant: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping("/producer/stats")
	public ResponseEntity<?> getProducerStats() {
		return ResponseEntity.ok().body(myJmsRestaurant.getStats());
	}

	@PostMapping("/consumer/start")
	public ResponseEntity<?> startConsumer() {
		metrics.reset();
		consumerOperations.start();
		return ResponseEntity.ok("Consumer started");
	}

	@PostMapping("/consumer/stop")
	public ResponseEntity<?> stopConsumer() {
		consumerOperations.stop();
		log.info("LISTENER TOTAL {}", metrics.getTotal());
		log.info("LISTENER CURRENT {}", metrics.getCurrent());
		return ResponseEntity.ok("Consumer stopped");
	}

	@GetMapping("/consumer/alive")
	public ResponseEntity<?> isConsumerRunning() {
		boolean running = consumerOperations.isRunning();
		return ResponseEntity.ok(running);
	}

	@GetMapping("/consumer/stats/total")
	public ResponseEntity<?> getSConsumerStatsTotal() {
		return ResponseEntity.ok().body(metrics.getTotal());
	}

	@GetMapping("/consumer/stats/current")
	public ResponseEntity<?> getSConsumerStatsCurrent() {
		return ResponseEntity.ok().body(metrics.getCurrent());
	}
}
