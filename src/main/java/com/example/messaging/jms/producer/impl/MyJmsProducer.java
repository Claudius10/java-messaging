package com.example.messaging.jms.producer.impl;

import com.example.messaging.common.exception.producer.ProducerClosedException;
import com.example.messaging.common.exception.producer.ProducerDeliveryException;
import com.example.messaging.common.exception.producer.ProducerSendException;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.util.Constants;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.util.MessageType;
import jakarta.jms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class MyJmsProducer implements Producer<Dish> {

	private final static Destination TEST_DESTINATION = new ActiveMQTopic("TEST_REQUEST");

	private final ConnectionFactory connectionFactory;

	private final JmsProperties jmsProperties;

	private final ProducerMetrics producerMetrics;

	private final CompletionListener completionListener;

	private JMSContext jmsContext;

	private JMSProducer jmsProducer;

	private Destination destination;

	private boolean open = false;

	private long currentMessage = 0;

	private long timeOfLastConnect = 0;

	@Override
	public void send(Dish dish) {
		try {
			checkOpen();
			Message message = createMessage(dish, MessageType.TEXT);
			setProperty(message, "id", currentMessage);
			doSend(message);
		} catch (ProducerDeliveryException ex) {
			producerMetrics.error();
			log.error("Failed to send message: '{}'", ex.getMessage(), ex);
			throw ex;
		}
	}

	private void checkOpen() {
		try {
			open();
		} catch (JMSRuntimeException ex) {
			String message = String.format("Failed to connect to JMS broker: '%s'", ex.getMessage());
			throw new ProducerClosedException(message, ex);
		}
	}

	private Message createMessage(Dish dish, MessageType type) {
		currentMessage = dish.getId();
		String content = dish.getName();

		try {
			switch (type) {
				case BYTES:
					BytesMessage bytesMessage = jmsContext.createBytesMessage();
					bytesMessage.writeBytes(content.getBytes());
					return bytesMessage;
				case OBJECT:
					return jmsContext.createObjectMessage(content);
				default:
					return jmsContext.createTextMessage(content);
			}
		} catch (Exception ex) {
			reconnectIfNecessary(ex);
			String message = String.format("Failed to create message '%s' with id '%s': '%s'", content, currentMessage, ex.getMessage());
			throw new ProducerDeliveryException(message, ex);
		}
	}

	private void setProperty(Message message, String name, Object value) {
		if (value == null) {
			return;
		}

		try {
			message.setStringProperty(name, value.toString());
		} catch (Exception ex) {
			String errorMessage = String.format("Failed to add property name '%s' and value '%s' to message '%s': '%s'", name, value, currentMessage, ex.getMessage());
			throw new ProducerDeliveryException(errorMessage, ex);
		}
	}

	private void doSend(Message message) {
		try {
			jmsProducer.send(destination, message);
		} catch (Exception ex) {
			reconnectIfNecessary(ex);
			String errorMessage = String.format("Failed to send message '%s' to destination '%s': '%s'", message, destination, ex.getMessage());
			throw new ProducerSendException(errorMessage, ex);
		}
	}

	private void open() {
		if (!open) {

			if (!openAllowed()) {
				// if not within connect/reconnect interval, throw to return and immediately back up message
				String message = String.format("Producer is disconnected, waiting for reconnection interval: %s seconds remaining",
						TimeUnit.MILLISECONDS.toSeconds(jmsProperties.getReconnectionIntervalMs() - getTimeElapsedSinceLastConnect()));
				throw new ProducerClosedException(message);
			}

			timeOfLastConnect = System.currentTimeMillis();

			if (log.isTraceEnabled()) log.trace("Attempting to establish connection with JMS broker...");

			jmsContext = connectionFactory.createContext(jmsProperties.getUser(), jmsProperties.getPassword(), Session.AUTO_ACKNOWLEDGE);

			jmsProducer = jmsContext.createProducer();
			jmsProducer.setAsync(completionListener);

			String jmsDestination = jmsProperties.getDestination();
			destination = jmsDestination.contains("queue") ? jmsContext.createQueue(jmsDestination) : jmsContext.createTopic(jmsDestination);

			open = true;
			if (log.isTraceEnabled()) log.trace("Connected to JMS broker");
		}
	}

	@Override
	public void close() {
		if (open) {
			try {
				// wait for last acks
				Thread.sleep(2500);
			} catch (InterruptedException ex) {
				log.error("Interrupted while waiting for ack before closing producer: '{}'", ex.getMessage());
			} finally {
				closeProducer();
				open = false;
			}
		}
	}

	private void closeProducer() {
		try {
			if (log.isTraceEnabled()) log.trace("Closing JMS producer...");
			jmsContext.close();
			if (log.isTraceEnabled()) log.trace("JMS producer disconnected");
		} catch (JMSRuntimeException ex) {
			log.warn("Failed to gracefully close connection: '{}'", ex.getMessage(), ex);
			this.jmsContext = null;
			this.jmsProducer = null;
		}
	}

	@Override
	public boolean isConnected() {
		try {
			checkOpen();
			jmsProducer.send(TEST_DESTINATION, Constants.TEST_REQUEST);
			return true;
		} catch (Exception ex) {
			reconnectIfNecessary(ex);
			return false;
		}
	}

	private void reconnectIfNecessary(Exception ex) {
		if (ex instanceof IllegalStateRuntimeException) {
			close();
		}
	}

	private boolean openAllowed() {
		return getTimeElapsedSinceLastConnect() > jmsProperties.getReconnectionIntervalMs();
	}

	private long getTimeElapsedSinceLastConnect() {
		return System.currentTimeMillis() - timeOfLastConnect;
	}
}
