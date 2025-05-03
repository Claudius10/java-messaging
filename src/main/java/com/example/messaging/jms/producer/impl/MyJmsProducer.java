package com.example.messaging.jms.producer.impl;

import com.example.messaging.common.exception.producer.ProducerClosedException;
import com.example.messaging.common.exception.producer.ProducerDeliveryException;
import com.example.messaging.common.exception.producer.ProducerSendException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
import jakarta.jms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;

@Slf4j
@RequiredArgsConstructor
public class MyJmsProducer implements Producer<Dish> {

	private final static Destination TEST_DESTINATION = new ActiveMQTopic("TEST_REQUEST");

	private final ConnectionFactory connectionFactory;

	private final JmsProperties jmsProperties;

	private JMSContext jmsContext;

	private JMSProducer jmsProducer;

	private Destination destination;

	private boolean open = false;

	@Override
	public void sendTextMessage(Dish dish) throws ProducerDeliveryException {
		checkOpen();

		Long id = dish.getId();
		String content = dish.getName();

		try {
			TextMessage message = jmsContext.createTextMessage(content);
			message.setLongProperty("id", id);
			jmsProducer.send(destination, message);
			if (log.isTraceEnabled()) log.trace("Sent message {} to destination {}", id, destination);
		} catch (JMSRuntimeException ex) {
			log.warn("Failed to send message {} to destination {}: {}", content, destination, ex.getMessage());
			close();
			throw new ProducerSendException();
		} catch (JMSException ex) {
			log.error("Failed to add property to message {} with id {}", content, id, ex);
		}
	}

	private void checkOpen() {
		try {
			connect();
		} catch (JMSRuntimeException ex) {
			log.warn("Failed to connect to JMS broker: {}", ex.getMessage());
			throw new ProducerClosedException();
		}
	}

	private void connect() {
		if (!open) {
			if (log.isTraceEnabled()) log.trace("Attempting to establish connection with JMS broker...");
			jmsContext = connectionFactory.createContext(jmsProperties.getUser(), jmsProperties.getPassword(), Session.AUTO_ACKNOWLEDGE);
			jmsProducer = jmsContext.createProducer();
			jmsProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
			jmsProducer.setAsync(new MyCompletionListener());
			destination = jmsProperties.getDestination().contains("queue") ? jmsContext.createQueue(jmsProperties.getDestination()) : jmsContext.createTopic(jmsProperties.getDestination());
			open = true;
			if (log.isTraceEnabled()) log.trace("Connected to JMS broker!");
		}
	}

	@Override
	public void close() {
		if (open) {
			try {
				if (log.isTraceEnabled()) log.trace("Closing JMS producer...");
				jmsContext.close();
				if (log.isTraceEnabled()) log.trace("JMS producer disconnected");
			} catch (JMSRuntimeException ex) {
				log.warn("Failed to gracefully close JMS broker: {}", ex.getMessage());
				this.jmsContext = null;
				this.jmsProducer = null;
			}
			open = false;
		}
	}

	@Override
	public boolean isConnected() {
		try {
			checkOpen();
			jmsProducer.send(TEST_DESTINATION, "TEST_REQUEST");
			return true;
		} catch (Exception ex) {
			close();
			return false;
		}
	}
}
