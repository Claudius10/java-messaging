package com.example.messaging.jms.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JmsConsumer implements Consumer {

	@Override
	@JmsListener(destination = "${jms.destination}", concurrency = "3")
	public void receive(String content) {
//		log.info("Received message: {}", content);
	}
}
