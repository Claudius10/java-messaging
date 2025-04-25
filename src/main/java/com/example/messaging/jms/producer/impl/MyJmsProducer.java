package com.example.messaging.jms.producer.impl;

import com.example.messaging.common.exception.ProducerDeliveryException;
import com.example.messaging.jms.producer.JmsProducer;
import jakarta.jms.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyJmsProducer implements JmsProducer {

	private final JMSContext jmsContext;

	private final JMSProducer jmsProducer;

	private final Destination destination;

	public MyJmsProducer(JMSContext jmsContext, JMSProducer jmsProducer, String destinationName) {
		this.jmsContext = jmsContext;
		this.jmsProducer = jmsProducer;
		this.destination = destinationName.contains("queue") ? jmsContext.createQueue(destinationName) : jmsContext.createTopic(destinationName);
	}

	@Override
	public void sendTextMessage(long id, String content) {
		try {
			TextMessage message = jmsContext.createTextMessage(content);
			message.setLongProperty("id", id);
			jmsProducer.send(destination, message);
			if (log.isTraceEnabled()) log.trace("Sent message {} to destination {}", id, destination);
		} catch (JMSException ex) {
			throw new ProducerDeliveryException(String.format("Failed to send message %s to topic %s", content, destination));
		}
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing JMS Producer...");
		jmsContext.close();
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
