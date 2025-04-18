package com.example.messaging.restaurant;

import com.example.messaging.util.DishesStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

//@Component
@RequiredArgsConstructor
@Slf4j
public class MyKafkaRestaurant extends MyBaseRestaurant implements Restaurant {

	public void open() {

	}

	public void close() {
	}

	@Override
	public Map<DishesStat, Long> getStats() {
		return Map.of();
	}

	@Override
	protected void createConsumers(int amount) {

	}

	@Override
	protected void createProducers(int amount) {

	}
}
