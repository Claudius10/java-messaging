package com.example.messaging.jms.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class JmsConsumerManager {

	private final JmsListenerEndpointRegistry registry;

	public void start(String id) {
		registry.getListenerContainer(id).start();
	}

	public void stop(String id) {
		registry.getListenerContainer(id).stop();
	}

	public boolean isRunning(String id) {
		return registry.getListenerContainer(id).isRunning();
	}
}
