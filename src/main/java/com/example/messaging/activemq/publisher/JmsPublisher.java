package com.example.messaging.activemq.publisher;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;

@Slf4j
@RequiredArgsConstructor
public class JmsPublisher implements Publisher {

	private final JmsTemplate jmsTemplate;

	@Override
	public void publishMessage(final Message message) {
		log.info("Publishing message to Artemis; content -> {}", message.getContent());
		jmsTemplate.send("testQueue", session -> session.createTextMessage("Hello World"));
	}
}
