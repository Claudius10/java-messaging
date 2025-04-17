package com.example.messaging.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JmsConsumer {

	@JmsListener(destination = "queue-table-A", concurrency = "2")
	public void receive(String content) {

	}
}
