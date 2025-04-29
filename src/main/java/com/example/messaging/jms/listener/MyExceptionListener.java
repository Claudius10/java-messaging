package com.example.messaging.jms.listener;

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.api.core.ActiveMQNotConnectedException;

@Slf4j
public class MyExceptionListener implements ExceptionListener {

	@Override
	public void onException(JMSException exception) {
		if (exception.getCause() instanceof ActiveMQNotConnectedException) {
			log.warn("Lost connection to ActiveMQ Artemis Broker");
			return;
		}

		log.error("JMS Exception occurred: {}", exception.getMessage());
	}
}
