package com.example.messaging.kafka.producer.impl;

import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.kafka.admin.MyKafkaAdmin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class MyKafkaProducer implements Producer<Dish> {

	private final String destination;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final MyKafkaAdmin myKafkaAdmin;

	private CompletableFuture<SendResult<Long, String>> pending;

	@Override
	public void send(Dish dish) {
		Long id = dish.getId();
		String content = dish.getName();

		try {
			pending = kafkaTemplate.send(destination, id, content);
		} catch (KafkaException e) {
			// ignore, kafka producer listener will handle backing up messages and updating metrics
		}
	}

	@Override
	public void close() {
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
			return myKafkaAdmin.clusterId() != null;
		} catch (KafkaException ex) {
			return false;
		}
	}
}
