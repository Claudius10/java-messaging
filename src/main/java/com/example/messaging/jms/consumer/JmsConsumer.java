package com.example.messaging.jms.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JmsConsumer implements Consumer {

	@Override
	@JmsListener(destination = "queue-table-A", concurrency = "2")
	public void receive(String content) {

	}
}
