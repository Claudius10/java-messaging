package com.example.messaging.restaurant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//@Component
@RequiredArgsConstructor
@Slf4j
public class MyKafkaRestaurant extends MyBaseRestaurant implements Restaurant {

	public void open() {

	}

	public void close() {
	}

	@Override
	protected void createConsumers(int amount) {

	}

	@Override
	protected void createProducers(int amount) {

	}
}
