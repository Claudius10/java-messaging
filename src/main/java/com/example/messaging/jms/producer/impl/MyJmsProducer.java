package com.example.messaging.jms.producer.impl;

import com.example.messaging.common.exception.producer.ProducerClosedException;
import com.example.messaging.common.exception.producer.ProducerDeliveryException;
import com.example.messaging.common.exception.producer.ProducerSendException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
import com.example.messaging.jms.util.MessageType;
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

	private long currentMessage = 0;

	private long timeOfLastConnect = 0;

	@Override
	public void send(Dish dish) throws ProducerDeliveryException {
		checkOpen();
		Message message = createMessage(dish, MessageType.TEXT);
		setProperty(message, "id", currentMessage);
		doSend(message);
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
			log.error("Failed to create message '{}' with 'id' {}: {}", content, currentMessage, ex.getMessage());
			reconnectIfNecessary(ex);
			throw new ProducerDeliveryException();
		}
	}

	private void setProperty(Message message, String name, Object value) {
		if (value == null) {
			return;
		}

		try {
			if (value instanceof String) {
				message.setStringProperty(name, (String) value);
			}

			if (value instanceof Long) {
				message.setLongProperty(name, (Long) value);
			}

			if (value instanceof Double) {
				message.setDoubleProperty(name, (Double) value);
			}
		} catch (Exception ex) {
			log.error("Failed to add property name '{}' and value '{}' to message '{}': {}", name, value, currentMessage, ex.getMessage());
			throw new ProducerDeliveryException();
		}
	}

	private void doSend(Message message) {
		try {
			jmsProducer.send(destination, message);
			if (log.isTraceEnabled()) log.trace("Sent message '{}' to destination '{}'", currentMessage, destination);
		} catch (Exception ex) {
			log.error("Failed to send message '{}' to destination '{}': {}", message, destination, ex.getMessage());
			reconnectIfNecessary(ex);
			throw new ProducerSendException();
		}
	}

	private void reconnectIfNecessary(Exception ex) {
		if (ex instanceof IllegalStateRuntimeException) {
			close();
		}
	}

	private boolean openAllowed() {
		long elapsed = System.currentTimeMillis() - timeOfLastConnect;
		return elapsed > jmsProperties.getReconnectionIntervalMs();
	}

	private void checkOpen() {
		try {
			open();
		} catch (JMSRuntimeException ex) {
			log.error("Failed to connect to JMS broker: {}", ex.getMessage());
			throw new ProducerClosedException();
		}
	}

	private void open() {
		if (!open) {

			if (!openAllowed()) {
				// if not within connect/reconnect interval, throw to return and immediately back up message
				throw new ProducerClosedException();
			}

			timeOfLastConnect = System.currentTimeMillis();

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
				log.warn("Failed to gracefully close connection: {}", ex.getMessage());
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
			reconnectIfNecessary(ex);
			return false;
		}
	}
}
