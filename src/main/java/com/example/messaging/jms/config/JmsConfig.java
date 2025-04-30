package com.example.messaging.jms.config;

import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.util.backoff.FixedBackOff;

@Profile("Jms")
@Configuration
@RequiredArgsConstructor
@EnableJms
public class JmsConfig {

	private final JmsProperties jmsProperties;

	@Bean
	ConnectionFactory connectionFactory() {
		JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));
		connectionFactory.setMaxConnections(jmsProperties.getMaxConnections() * 2 + 1); // used for the ListenerContainerFactory (Consumers) and for creating JMSContext (Producers), and 1 for the
		// schedules backup process
		return connectionFactory;
	}

	@Bean
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setClientId(jmsProperties.getConsumerClientId());
		factory.setBackOff(new FixedBackOff(jmsProperties.getReconnectionIntervalMs(), 9999));
		factory.setConnectionFactory(connectionFactory);
		factory.setConcurrency(String.valueOf(jmsProperties.getMaxConnections()));
		factory.setAutoStartup(false);
		return factory;
	}
}
