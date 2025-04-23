package com.example.messaging.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@Slf4j
public class KafkaConsumer {

	@KafkaListener(id = "${kafka.consumer-id}", groupId = "${kafka.consumer-group-id}", topics = "${kafka.topic}")
	public void receive(ConsumerRecord<Integer, String> record) {
		if (log.isTraceEnabled()) {
			log.trace("Message topic: {}", record.topic());
			log.trace("Message topic partition: {}", record.partition());
			log.trace("Message offset: {}", record.offset());
			log.trace("Message key: {}", record.key());
			log.trace("Message content: {}", record.value());
		}
	}
}
