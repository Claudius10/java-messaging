package com.example.messaging.activemq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ActiveMQListener {

	// If no JmsListenerContainerFactory has been defined, a default one is configured automatically. @EnableJms manual config -> https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/jms/annotation/EnableJms.html
	// By default, the default factory is transactional.

	// If you run in an infrastructure where a JtaTransactionManager is present, it is associated to the listener container by
	// default. If not, the sessionTransacted flag is enabled.
	// In that latter scenario, you can associate your local data store transaction to the processing of an incoming message by adding @Transactional on your listener method (or a delegate thereof).
	// this ensures that the incoming message is acknowledged, once the local transaction has completed.
	// This also includes sending response messages that have been performed on the same JMS session.


	// ExceptionListener

//	@JmsListener(destination = "testQueue")
//	@Transactional
//	public void read(String content) {
//		log.info("Received message: {}", content);
//	}
}
