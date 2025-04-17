package com.example.messaging.producer;

import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;

public interface Producer {

	void sendTextMessage(long id, String content) throws JMSException;

	JMSContext getContext();

	JMSProducer getProducer();
}
