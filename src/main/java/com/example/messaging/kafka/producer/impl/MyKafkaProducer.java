package com.example.messaging.kafka.producer.impl;

import com.example.messaging.kafka.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class MyKafkaProducer implements KafkaProducer {

	private final String topic;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	@Override
	public void sendTextMessage(long key, String value) {
		kafkaTemplate.send(topic, key, value);
		if (log.isTraceEnabled()) log.trace("Sent message {} to topic {}", key, topic);
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
