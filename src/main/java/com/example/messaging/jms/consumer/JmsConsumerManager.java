package com.example.messaging.jms.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class JmsConsumerManager {

	private final JmsListenerEndpointRegistry internalJmsListenerEndpointRegistry;

	public void start() {
		internalJmsListenerEndpointRegistry.getListenerContainers().forEach(Lifecycle::start);
	}

	public void stop() {
		internalJmsListenerEndpointRegistry.getListenerContainers().forEach(Lifecycle::stop);
	}

	public boolean isRunning() {
		return internalJmsListenerEndpointRegistry.isRunning();
	}
}
