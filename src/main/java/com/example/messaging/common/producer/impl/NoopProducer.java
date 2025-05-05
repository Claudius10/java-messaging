package com.example.messaging.common.producer.impl;

import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;

public class NoopProducer implements Producer<Dish> {

	@Override
	public void send(Dish dish) {
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isConnected() {
		return false;
	}
}
