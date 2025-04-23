package com.example.messaging.kafka.controller;

import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.consumer.KafkaConsumerOperations;
import com.example.messaging.common.restaurant.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
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

	private final Restaurant myKafkaRestaurant;

	private final KafkaProperties kafkaProperties;

	private final KafkaConsumerOperations consumerOperations;

	@PostMapping("/open")
	public ResponseEntity<?> open() {
		log.info("Opening Kafka restaurant...");
		myKafkaRestaurant.open();
		consumerOperations.start(kafkaProperties.getConsumerId());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/close")
	public ResponseEntity<?> close() {
		try {
			log.info("Closing Kafka restaurant...");
			myKafkaRestaurant.close();
			consumerOperations.stop(kafkaProperties.getConsumerId());
			return ResponseEntity.ok().build();
		} catch (InterruptedException e) {
			log.error("Interrupted with closing Kafka Restaurant: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping("/stats")
	public ResponseEntity<?> getStats() {
		return ResponseEntity.ok().body(myKafkaRestaurant.getStats());
	}
}
