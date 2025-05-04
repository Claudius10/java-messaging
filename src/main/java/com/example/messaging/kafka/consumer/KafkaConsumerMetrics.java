package com.example.messaging.kafka.consumer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Profile("Kafka")
@Component
public class KafkaConsumerMetrics {

	private final AtomicLong total = new AtomicLong(0);

	private final AtomicLong current = new AtomicLong(0);

	public void increment() {
		current.incrementAndGet();
		total.incrementAndGet();
	}

	public long getCurrent() {
		return current.get();
	}

	public long getTotal() {
		return total.get();
	}

	public void reset() {
		if (current.get() > 0) {
			current.set(0);
		}
	}
}
