package com.example.messaging.util;

import com.example.messaging.config.MyCompletionListener;
import jakarta.jms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JmsConnection {

	private final JmsProperties jmsProperties;

	private JMSContext jmsContext;

	private JMSProducer jmsProducer;

	private Destination destination;

	public void send(long id) throws JMSException {
		TextMessage message = jmsContext.createTextMessage("Delicious dish");
		message.setLongProperty("id", id);
		jmsProducer.send(destination, message);
	}

	public void connect(ConnectionFactory jmsConnectionFactory, ExceptionListener exceptionListener, String destinationName) {
		jmsContext = jmsConnectionFactory.createContext(jmsProperties.getUser(), jmsProperties.getPassword(), Session.AUTO_ACKNOWLEDGE);
		jmsContext.setExceptionListener(exceptionListener);
		jmsProducer = jmsContext.createProducer();
		jmsProducer.setAsync(new MyCompletionListener()); // allows receiving ack async, so no blocking on send

		if (destinationName.contains("queue")) {
			destination = jmsContext.createQueue(destinationName);
		} else {
			destination = jmsContext.createTopic(destinationName);
		}

		log.info("Connected to Artemis Broker");
	}
}
