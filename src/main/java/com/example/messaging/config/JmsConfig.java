package com.example.messaging.config;

import com.example.messaging.util.JmsProperties;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JmsConfig {

	private final JmsProperties jmsProperties;

	@Bean
	ConnectionFactory jmsPoolConnectionFactory() {
		JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory());
		connectionFactory.setMaxConnections(Integer.parseInt(jmsProperties.getMaxConnections()));
		return connectionFactory;
	}
}
