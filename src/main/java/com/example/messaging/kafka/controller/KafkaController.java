package com.example.messaging.kafka.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.consumer.KafkaConsumerOperations;
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

	private final KafkaConsumerOperations consumerOperations;

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

	@PostMapping("/consumer/start")
	public ResponseEntity<?> startConsumer() {
		try {
			consumerOperations.start(kafkaProperties.getConsumerId());
		} catch (KafkaException ex) {
			return ResponseEntity.ok(ex.getMessage());
		}

		return ResponseEntity.ok("Consumer started");
	}

	@PostMapping("/consumer/stop")
	public ResponseEntity<?> stopConsumer() {
		try {
			consumerOperations.stop(kafkaProperties.getConsumerId());
		} catch (KafkaException ex) {
			return ResponseEntity.ok(ex.getMessage());
		}

		return ResponseEntity.ok("Consumer stopped");
	}

	@GetMapping("/stats")
	public ResponseEntity<?> getStats() {
		return ResponseEntity.ok().body(myKafkaRestaurant.getStats());
	}
}
