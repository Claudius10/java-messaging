package com.example.messaging.restaurant;

import com.example.messaging.util.DishesStat;

import java.util.Map;

public interface Restaurant {

	void open();

	void close() throws InterruptedException;

	Map<DishesStat, Long> getStats();
}
