package com.example.messaging.kafka.producer.impl;

import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class MyKafkaProducer implements Producer<Dish> {

	private final static String TEST_DESTINATION = "TEST_TOPIC";

	private final String destination;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private CompletableFuture<SendResult<Long, String>> pending;

	@Override
	public void send(Dish dish) {
		Long id = dish.getId();
		String content = dish.getName();

		try {
			pending = kafkaTemplate.send(destination, id, content);
		} catch (KafkaException ex) {
			// ignore, kafka producer listener will handle backing up messages and updating metrics
		}
	}

	@Override
	public void close() {
		try {
			// wait for last acks
			Thread.sleep(2500);
		} catch (InterruptedException ex) {
			log.error("Interrupted while waiting for ack before closing producer: '{}'", ex.getMessage());
		} finally {
			closeProducer();
		}
	}

	private void closeProducer() {
		if (log.isTraceEnabled()) log.trace("Closing Kafka Producer...");

		if (pending == null) {
			kafkaTemplate.getProducerFactory().closeThreadBoundProducer();
			return;
		}

		pending.whenComplete((_, _) -> kafkaTemplate.getProducerFactory().closeThreadBoundProducer());
	}

	@Override
	public boolean isConnected() {
		try {
			kafkaTemplate.send(TEST_DESTINATION, Constants.TEST_REQUEST).get(10, TimeUnit.SECONDS);
			return true;
		} catch (InterruptedException ex) {
			log.error("Interrupted while checking broker connection status: '{}'", ex.getMessage());
			return false;
		} catch (Exception ex) {
			return false;
		}
	}
}
