package com.example.messaging.kafka.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.consumer.KafkaConsumerManager;
import com.example.messaging.kafka.consumer.KafkaConsumerMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("Kafka")
@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaController {

	private final MessagingManager myKafkaRestaurant;

	private final KafkaProperties kafkaProperties;

	private final KafkaConsumerManager consumerOperations;

	private final KafkaConsumerMetrics metrics;

	@PostMapping("/producer/start")
	public ResponseEntity<?> startProducer() {
		log.info("Opening Kafka restaurant...");
		myKafkaRestaurant.open();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/producer/stop")
	public ResponseEntity<?> stopProducer() {
		try {
			log.info("Closing Kafka restaurant...");
			myKafkaRestaurant.close();
			return ResponseEntity.ok().build();
		} catch (InterruptedException e) {
			log.error("Interrupted with closing Kafka Restaurant: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping("/producer/stats")
	public ResponseEntity<?> getStats() {
		return ResponseEntity.ok().body(myKafkaRestaurant.getStats());
	}

	@PostMapping("/consumer/start")
	public ResponseEntity<?> startConsumer() {
		try {
			metrics.reset();
			consumerOperations.start(kafkaProperties.getConsumerClientId());
		} catch (KafkaException ex) {
			return ResponseEntity.ok(ex.getMessage());
		}

		return ResponseEntity.ok("Consumer started");
	}

	@PostMapping("/consumer/stop")
	public ResponseEntity<?> stopConsumer() {
		try {
			consumerOperations.stop(kafkaProperties.getConsumerClientId());
			log.info("LISTENER TOTAL {}", metrics.getTotal());
			log.info("LISTENER CURRENT {}", metrics.getCurrent());
		} catch (KafkaException ex) {
			return ResponseEntity.ok(ex.getMessage());
		}

		return ResponseEntity.ok("Consumer stopped");
	}

	@GetMapping("/consumer/alive")
	public ResponseEntity<?> isConsumerRunning() {
		boolean running = consumerOperations.isRunning(kafkaProperties.getConsumerClientId());
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
