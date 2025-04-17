package com.example.messaging.jms;

import jakarta.jms.*;
import org.springframework.stereotype.Component;

// Jakarta Messaging 2.0

@Component
public class JmsConnectionFactory {

	private final ConnectionFactory connectionFactory;

	public JmsConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public JMSContext createContext(String username, String password, int session) {
		return connectionFactory.createContext(username, password, session);
	}
}
