package com.example.messaging.common.metrics;

import com.example.messaging.common.util.ConsumerMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ConsumerMetrics {

	private final AtomicLong total = new AtomicLong(0);

	private final AtomicLong current = new AtomicLong(0);

	public void increment() {
		current.getAndIncrement();
		total.getAndIncrement();
	}

	public Map<ConsumerMetric, Long> getMetrics() {
		Map<ConsumerMetric, Long> metrics = new HashMap<>();
		metrics.put(ConsumerMetric.TOTAL, total.get());
		metrics.put(ConsumerMetric.CURRENT, current.get());
		return metrics;
	}

	public void print() {
		getMetrics().forEach((consumerMetric, value) -> log.info("{}: {}", consumerMetric.name(), value));
	}

	public synchronized void reset() {
		if (current.get() > 0) {
			current.set(0);
		}
	}
}
