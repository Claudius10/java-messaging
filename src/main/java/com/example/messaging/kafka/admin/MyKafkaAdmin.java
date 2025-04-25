package com.example.messaging.kafka.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyKafkaAdmin {

	private final KafkaAdmin admin;

	public String clusterId() {
		log.info("Attempting communicate with the Kafka broker. Timeout in 30 seconds.");

		String id = admin.clusterId();

		if (id == null) {
			throw new KafkaException("Error communicating with the broker");
		}

		if (log.isTraceEnabled()) log.trace("Cluster id: {}", id);

		return id;
	}
}
