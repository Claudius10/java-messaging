package com.example.messaging.activemq.publisher;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class NoopPublisher implements Publisher {

	// ConnectionFactory->Connection->Session->MessageProducer->send

	@Override
	public void publishMessage(final Message message) {
		log.info("Publishing message to ?: {}", message.getContent());
		// send the message somewhere: DB, file, another JMS destination, etc.
	}
}
