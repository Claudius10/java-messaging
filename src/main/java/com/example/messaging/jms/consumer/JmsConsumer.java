package com.example.messaging.jms.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@Slf4j
public class JmsConsumer {

	@JmsListener(id = "${jms.consumer-client-id}", destination = "${jms.destination}", concurrency = "3")
	public void receive(String content) {
		if (log.isTraceEnabled()) log.trace("Received message: {}", content);
	}
}
