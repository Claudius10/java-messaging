package com.example.messaging.jms.config;

import com.example.messaging.jms.listener.MyExceptionListener;
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

		final int producersAndConsumersPair = jmsProperties.getMaxConnections() * 2;
		final int listeners = jmsProperties.getMaxConnections();
		final int scheduledTasksThatNeedAJmsConnection = 1;
		final int maxConnections = producersAndConsumersPair + listeners + scheduledTasksThatNeedAJmsConnection;

		connectionFactory.setMaxConnections(maxConnections);
		return connectionFactory;
	}

	@Bean
	DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setBackOff(new FixedBackOff(jmsProperties.getReconnectionIntervalMs(), 9999));
		factory.setConnectionFactory(connectionFactory);
		final String maxConnections = String.valueOf(jmsProperties.getMaxConnections());
		factory.setConcurrency(maxConnections);
		factory.setAutoStartup(false);
		factory.setExceptionListener(new MyExceptionListener());
		return factory;
	}
}
