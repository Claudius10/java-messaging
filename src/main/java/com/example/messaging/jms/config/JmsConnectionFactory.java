package com.example.messaging.jms.config;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Jakarta Messaging 2.0

@Profile("Jms")
@Component
@RequiredArgsConstructor
public class JmsConnectionFactory {

	private final ConnectionFactory connectionFactory;

	public JMSContext createContext(String username, String password, int session) {
		return connectionFactory.createContext(username, password, session);
	}
}
