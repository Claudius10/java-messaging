package com.example.messaging.common.producer.impl;

import com.example.messaging.common.producer.Producer;

public class NoopProducer implements Producer {

	@Override
	public void sendTextMessage(long id, String content) {
	}

	@Override
	public void close() {
	}
}
