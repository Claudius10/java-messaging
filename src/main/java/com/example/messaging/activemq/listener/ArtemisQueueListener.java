package com.example.messaging.activemq.listener;

import com.example.messaging.activemq.Listener;
import com.example.messaging.model.Message;
import com.example.messaging.util.MessageQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArtemisQueueListener implements Listener {

	private final MessageQueue messageQueue;

	@JmsListener(destination = "testQueue", concurrency = "10")
	public void receive(String content) {
		Message message = Message.builder().withContent(content).build();
		log.info("Received message: {}", message.getContent());
		boolean result = messageQueue.offer(message);

		if (!result) {
			log.warn("Failed to offer message: {}", content);
		}
	}
}
