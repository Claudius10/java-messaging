package com.example.messaging.config;

import com.example.messaging.util.JmsProperties;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
@RequiredArgsConstructor
@EnableJms
public class JmsConfig {

	private final JmsProperties jmsProperties;

	@Bean
	ConnectionFactory connectionFactory() {
		JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		connectionFactory.setConnectionFactory(resolveConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));
		connectionFactory.setMaxConnections(Integer.parseInt(jmsProperties.getMaxConnections()));
		return connectionFactory;
	}

	@Bean
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setSessionTransacted(true);
		factory.setConcurrency("3");
		return factory;
	}

	private ConnectionFactory resolveConnectionFactory(String url, String username, String password) {
		switch (jmsProperties.getFactory()) {
			case "ActiveMQConnectionFactory":
				return new ActiveMQConnectionFactory(url, username, password);
			default:
				return new ActiveMQConnectionFactory(url, username, password);
		}
	}
}
