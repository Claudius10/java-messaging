package com.example.messaging.kafka.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.metrics.ConsumerMetrics;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.consumer.KafkaConsumerManager;
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

	private final ConsumerMetrics consumerMetrics;

	private final ProducerMetrics producerMetrics;

	@PostMapping("/producer/start")
	public ResponseEntity<?> startProducer() {
		log.info("Opening Kafka restaurant...");
		producerMetrics.reset();
		myKafkaRestaurant.open();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/producer/stop")
	public ResponseEntity<?> stopProducer() {
		try {
			log.info("Closing Kafka restaurant...");
			myKafkaRestaurant.close();
			producerMetrics.print();
			return ResponseEntity.ok().build();
		} catch (InterruptedException e) {
			log.error("Interrupted with closing Kafka Restaurant: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping("/producer/metrics")
	public ResponseEntity<?> getStats() {
		return ResponseEntity.ok().body(producerMetrics.getMetrics());
	}

	@PostMapping("/consumer/start")
	public ResponseEntity<?> startConsumer() {
		try {
			consumerMetrics.reset();
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
			consumerMetrics.print();
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

	@GetMapping("/consumer/metrics")
	public ResponseEntity<?> getSConsumerStatsTotal() {
		return ResponseEntity.ok().body(consumerMetrics.getMetrics());
	}
}
