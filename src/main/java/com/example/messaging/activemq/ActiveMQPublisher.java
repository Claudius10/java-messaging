package com.example.messaging.activemq;

import com.example.messaging.contracts.Publisher;
import com.example.messaging.model.JmsTextMessageDTO;
import jakarta.jms.Destination;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.jms.core.JmsTemplate;

@RequiredArgsConstructor
@Setter
public class ActiveMQPublisher implements Publisher {

	private final JmsTemplate jmsTemplate;

	private final Destination destination;

	@Override
	public void publishMessage(final JmsTextMessageDTO message) {
		jmsTemplate.send(destination, session -> {
			TextMessage textMessage = session.createTextMessage(message.getContent());
			textMessage.setIntProperty("id", message.getId());
			return textMessage;
		});
	}


}
