package com.example.messaging.activemq.publisher;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import jakarta.jms.Destination;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Setter
public class JmsQueuePublisher implements Publisher {

	private final JmsTemplate jmsTemplate;

	private Destination destination;

	@Override
	public void publishMessage(final Message message) throws JmsException {
		String content = message.getContent();
		long id = message.getId();
		log.info("Publishing message: {} - {}", id, content);
		jmsTemplate.send(destination, session -> {
			TextMessage textMessage = session.createTextMessage(content);
			textMessage.setLongProperty("id", message.getId());
			return textMessage;
		});
	}
}