package com.example.messaging.common.manager;

import com.example.messaging.common.util.MessagingStat;

import java.util.Map;

public interface MessagingManager {

	void open();

	void close() throws InterruptedException;

	boolean isProducing();

	boolean isConsuming();

	Map<MessagingStat, Long> getStats();
}
