package com.example.messaging.kafka.producer.impl;

import com.example.messaging.common.exception.producer.ProducerDeliveryException;
import com.example.messaging.common.exception.producer.ProducerSendException;
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

	private final String topic;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final MyKafkaAdmin myKafkaAdmin;

	private CompletableFuture<SendResult<Long, String>> sendFuture;

	@Override
	public void sendTextMessage(Dish dish) throws ProducerDeliveryException {
		try {
			sendFuture = kafkaTemplate.send(topic, dish.getId(), dish.getName());
		} catch (KafkaException ex) {
			throw new ProducerSendException();
		}
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing Kafka Producer...");
		sendFuture.whenComplete((result, ex) -> {
			kafkaTemplate.getProducerFactory().reset();
		});
	}

	@Override
	public boolean isConnected() {
		return myKafkaAdmin.clusterId() != null;
	}
}
