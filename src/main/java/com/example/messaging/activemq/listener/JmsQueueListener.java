package com.example.messaging.activemq.listener;

import com.example.messaging.activemq.Listener;
import com.example.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;

//@Component
@RequiredArgsConstructor
@Slf4j
public class JmsQueueListener implements Listener {

	@JmsListener(destination = "testQueue", concurrency = "10")
	public void receive(String content) {
		Message message = Message.builder().withContent(content).build();
		log.info("Received message: {}", message.getContent());
	}
}
