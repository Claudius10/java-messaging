package com.example.messaging.common.manager;

import com.example.messaging.common.util.MessagingMetric;

import java.util.Map;

public interface MessagingManager {

	void open();

	void close() throws InterruptedException;

	boolean isProducing();

	boolean isConsuming();

	Map<MessagingMetric, Long> getMetrics();
}
