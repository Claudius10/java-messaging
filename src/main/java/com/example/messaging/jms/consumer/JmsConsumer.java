package com.example.messaging.jms.consumer;

import com.example.messaging.common.metrics.ConsumerMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class JmsConsumer {

	private final ConsumerMetrics metrics;

	@JmsListener(destination = "${jms.destination}", concurrency = "${jms.max-connections}")
	public void receive(String content) {
		if (log.isTraceEnabled()) log.trace("Received message: '{}'", content);
		metrics.increment();
	}
}
