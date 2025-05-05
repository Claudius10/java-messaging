package com.example.messaging.common.metrics;

import com.example.messaging.common.util.ProducerMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ProducerMetrics {

	private final AtomicLong total = new AtomicLong(0);

	private final AtomicLong sent = new AtomicLong(0);

	private final AtomicLong error = new AtomicLong(0);

	private final AtomicLong resent = new AtomicLong(0);

	public void sent() {
		sent.incrementAndGet();
		total.incrementAndGet();
	}

	public void error() {
		error.incrementAndGet();
		total.incrementAndGet();
	}

	public void resent() {
		resent.incrementAndGet();
	}

	public Map<ProducerMetric, Long> getMetrics() {
		Map<ProducerMetric, Long> metrics = new HashMap<>();
		metrics.put(ProducerMetric.TOTAL, total.get());
		metrics.put(ProducerMetric.SENT, sent.get());
		metrics.put(ProducerMetric.ERROR, error.get());
		metrics.put(ProducerMetric.RESENT, resent.get());
		return metrics;
	}

	public void print() {
		getMetrics().forEach((consumerMetric, value) -> log.info("{}: {}", consumerMetric.name(), value));
	}

	public synchronized void reset() {
		if (sent.get() > 0) {
			sent.set(0);
		}

		if (error.get() > 0) {
			error.set(0);
		}

		if (resent.get() > 0) {
			resent.set(0);
		}
	}
}
