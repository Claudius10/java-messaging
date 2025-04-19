package com.example.messaging.jms.producer;

import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;

public class NoopProducer implements Producer {

	@Override
	public void sendTextMessage(long id, String content) {

	}

	@Override
	public JMSContext getContext() {
		return null;
	}

	@Override
	public JMSProducer getProducer() {
		return null;
	}
}
