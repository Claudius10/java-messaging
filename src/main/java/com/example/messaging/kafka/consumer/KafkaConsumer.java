package com.example.messaging.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@RequiredArgsConstructor
@Component
@Slf4j
public class KafkaConsumer {

	private final KafkaConsumerMetrics metrics;

	@KafkaListener(id = "${kafka.consumer-client-id}", groupId = "${kafka.consumer-group-id}", topics = "${kafka.topic}")
	public void receive(ConsumerRecord<Integer, String> record) {
		metrics.increment();
		if (log.isTraceEnabled()) {
			log.trace("Received message: topic -> {} - partition -> {} - offset -> {} - key -> {} - content -> {}",
					record.topic(),
					record.partition(),
					record.offset(),
					record.key(),
					record.value());
		}
	}
}
