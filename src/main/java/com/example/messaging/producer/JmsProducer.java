package com.example.messaging.producer;

import jakarta.jms.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JmsProducer implements Producer {

	private final JMSContext jmsContext;

	private final JMSProducer jmsProducer;

	private final Destination destination;

	public JmsProducer(JMSContext jmsContext, String destinationName) {
		this.jmsContext = jmsContext;
		this.jmsProducer = jmsContext.createProducer();
		this.destination = destinationName.contains("queue") ? jmsContext.createQueue(destinationName) : jmsContext.createTopic(destinationName);
	}

	@Override
	public void sendTextMessage(long id, String content) throws JMSException {
		TextMessage message = jmsContext.createTextMessage(content);
		message.setLongProperty("id", id);
		jmsProducer.send(destination, message);
	}

	@Override
	public JMSContext getContext() {
		return jmsContext;
	}

	@Override
	public JMSProducer getProducer() {
		return jmsProducer;
	}
}
