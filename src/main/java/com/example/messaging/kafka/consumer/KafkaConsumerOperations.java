package com.example.messaging.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerOperations {

	private final KafkaListenerEndpointRegistry registry;

	public void start(String id) {
		log.info("Starting Kafka consumer {}", id);
		registry.getListenerContainer(id).start();
	}

	public void stop(String id) {
		log.info("Stopping Kafka consumer {}", id);
		registry.getListenerContainer(id).stop();
	}

	public boolean isRunning(String id) {
		return registry.getListenerContainer(id).isRunning();
	}
}
