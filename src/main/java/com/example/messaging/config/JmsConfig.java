package com.example.messaging.config;

import com.example.messaging.util.JmsProperties;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JmsConfig {

	// JMSContext, JMSProducer, etc., -> JMS 2.0
	// --> to receive async ack, gotta use  completion listener with confirmation window prop on url and then no need for setBlockOnDurableSend(false);
	// --> confirm performance diff between jmsTemplate and manual with JmsConnection

	private final JmsProperties jmsProperties;

	ConnectionFactory getConnectionFactory() throws JMSException {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(jmsProperties.getBrokerUrl());
		activeMQConnectionFactory.setUser(jmsProperties.getUser());
		activeMQConnectionFactory.setPassword(jmsProperties.getPassword());
//		activeMQConnectionFactory.setBlockOnDurableSend(false);
		return activeMQConnectionFactory;
	}

	@Bean
	ConnectionFactory jmsPoolConnectionFactory() throws JMSException {
		JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		connectionFactory.setConnectionFactory(getConnectionFactory());
		connectionFactory.setMaxConnections(Integer.parseInt(jmsProperties.getMaxConnections()));
		return connectionFactory;
	}

//	@Bean
//	JmsTemplate jmsTemplate() throws JMSException {
//		JmsTemplate jmsTemplate = new JmsTemplate(jmsPoolConnectionFactory());
//		jmsTemplate.setDefaultDestination(new ActiveMQQueue(jmsProperties.getDestination()));
//		return jmsTemplate;
//	}
}
