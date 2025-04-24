package com.example.messaging.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerOperations {

	private final KafkaListenerEndpointRegistry registry;

	private final KafkaAdmin admin;

	public void start(String id) {
		if (clusterId() != null) {
			log.info("Starting Kafka consumer {}", id);
			registry.getListenerContainer(id).start();
		}
	}

	public void stop(String id) {
		if (clusterId() != null) {
			log.info("Stopping Kafka consumer {}", id);
			registry.getListenerContainer(id).stop();
		}
	}

	public String clusterId() {
		log.info("Attempting communicate with the Kafka broker. Timeout in 30 seconds.");

		String id = admin.clusterId();

		if (id == null) {
			throw new KafkaException("Error communicating with broker");
		}

		if (log.isTraceEnabled()) log.trace("Cluster id: {}", id);

		return id;
	}

	public boolean isRunning(String id) {
		return registry.getListenerContainer(id).isRunning();
	}
}
