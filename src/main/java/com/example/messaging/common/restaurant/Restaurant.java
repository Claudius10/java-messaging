package com.example.messaging.common.restaurant;

import com.example.messaging.common.util.DishesStat;

import java.util.Map;

public interface Restaurant {

	void open();

	void close() throws InterruptedException;

	boolean isCooking();

	boolean isServing();

	Map<DishesStat, Long> getStats();
}
