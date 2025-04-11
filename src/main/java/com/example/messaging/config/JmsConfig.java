package com.example.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;


@Configuration
@EnableJms
public class JmsConfig {

//	@Bean
//	DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory, DestinationResolver destinationResolver) {
//		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//		factory.setConnectionFactory(connectionFactory);
//		factory.setDestinationResolver(destinationResolver);
//		factory.setSessionTransacted(true);
//		factory.setConcurrency("10");
//		return factory;
//	}
//
//	@Bean
//	DestinationResolver destinationResolver() {
//		return new DynamicDestinationResolver();
//	}
}
