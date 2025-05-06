package com.example.messaging.jms.controller;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.metrics.ConsumerMetrics;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.jms.backup.JmsCheckBackup;
import com.example.messaging.jms.consumer.JmsConsumerManager;
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

	private final ConsumerMetrics consumerMetrics;

	private final ProducerMetrics producerMetrics;

	private final JmsCheckBackup jmsCheckBackup;

	@PostMapping("/producer/start")
	public ResponseEntity<?> startProducer() {
		log.info("Opening JMS restaurant...");
		producerMetrics.reset();
		myJmsRestaurant.open();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/producer/stop")
	public ResponseEntity<?> stopProducer() {
		try {
			log.info("Closing JMS restaurant...");
			myJmsRestaurant.close();
			// wait for last acks before printing
			Thread.sleep(2500);
			producerMetrics.print();
			return ResponseEntity.ok().build();
		} catch (InterruptedException e) {
			log.error("Interrupted with closing JMS Restaurant: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@GetMapping("/producer/metrics")
	public ResponseEntity<?> getProducerStats() {
		return ResponseEntity.ok().body(producerMetrics.getMetrics());
	}

	@PostMapping("/backup")
	public ResponseEntity<?> checkBackup() {
		jmsCheckBackup.backupCheck();
		return ResponseEntity.ok().build();
	}

	@PostMapping("/consumer/start")
	public ResponseEntity<?> startConsumer() {
		consumerMetrics.reset();
		consumerOperations.start();
		return ResponseEntity.ok("Consumer started");
	}

	@PostMapping("/consumer/stop")
	public ResponseEntity<?> stopConsumer() {
		consumerOperations.stop();
		consumerMetrics.print();
		return ResponseEntity.ok("Consumer stopped");
	}

	@GetMapping("/consumer/alive")
	public ResponseEntity<?> isConsumerRunning() {
		boolean running = consumerOperations.isRunning();
		return ResponseEntity.ok(running);
	}

	@GetMapping("/consumer/metrics")
	public ResponseEntity<?> getSConsumerStatsTotal() {
		return ResponseEntity.ok().body(consumerMetrics.getMetrics());
	}
}
