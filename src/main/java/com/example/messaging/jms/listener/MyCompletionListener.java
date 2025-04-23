package com.example.messaging.jms.listener;

import jakarta.jms.CompletionListener;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@Slf4j
public class MyCompletionListener implements CompletionListener {

	@Override
	public void onCompletion(Message message) {
//		if (log.isTraceEnabled()) log.trace("Acknowledged");
	}

	@Override
	public void onException(Message message, Exception exception) {
		log.error("Error sending JMS message: {}", exception.getMessage());
	}
}