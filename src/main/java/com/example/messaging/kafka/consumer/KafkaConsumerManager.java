package com.example.messaging.kafka.consumer;

import com.example.messaging.kafka.admin.MyKafkaAdmin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerManager {

	private final KafkaListenerEndpointRegistry registry;

	private final MyKafkaAdmin kafkaAdmin;

	public void start(String id) {
		if (kafkaAdmin.clusterId() != null) {
			log.info("Starting Kafka consumer {}", id);
			registry.getListenerContainer(id).start();
		}
	}

	public void stop(String id) {
		if (kafkaAdmin.clusterId() != null) {
			log.info("Stopping Kafka consumer {}", id);
			registry.getListenerContainer(id).stop();
		}
	}

	public boolean isRunning(String id) {
		return registry.getListenerContainer(id).isRunning();
	}
}
