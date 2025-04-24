package com.example.messaging.kafka.producer.impl;

import com.example.messaging.common.exception.ProducerDeliveryException;
import com.example.messaging.kafka.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class MyKafkaProducer implements KafkaProducer {

	private final String topic;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	@Override
	public void sendTextMessage(long key, String value) throws ProducerDeliveryException {
		try {
			kafkaTemplate.send(topic, key, value);
		} catch (KafkaException ex) {
			throw new ProducerDeliveryException(String.format("Failed to send message %s to topic %s", value, topic));
		}
	}

	@Override
	public KafkaTemplate<Long, String> getTemplate() {
		return kafkaTemplate;
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing Kafka Producer...");
		kafkaTemplate.getProducerFactory().closeThreadBoundProducer();
	}
}
