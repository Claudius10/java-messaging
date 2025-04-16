package com.example.messaging.util;

import com.example.messaging.config.MyCompletionListener;
import jakarta.jms.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;

@Slf4j
public class JmsConnection {

	private JMSContext jmsContext;

	private JMSProducer jmsProducer;

	private Destination destination;

	public void send(long id) throws JMSException {
		TextMessage message = jmsContext.createTextMessage("Delicious dish");
		message.setLongProperty("id", id);
		jmsProducer.send(destination, message);
	}

	public void connect(ConnectionFactory jmsConnectionFactory, ExceptionListener exceptionListener, String destinationName) {
		jmsContext = jmsConnectionFactory.createContext();
		jmsProducer = jmsContext.createProducer();
		jmsProducer.setAsync(new MyCompletionListener()); // --> allows receiving ack async so no blocking on send
		destination = jmsContext.createQueue(destinationName);
		log.info("Connected to Artemis Broker");
	}

	private Destination resolveDestination(String destination) {
		if (destination.contains("queue")) {
			return new ActiveMQQueue(destination);
		} else {
			return new ActiveMQTopic(destination);
		}
	}
}
