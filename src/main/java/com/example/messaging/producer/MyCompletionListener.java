package com.example.messaging.producer;

import jakarta.jms.CompletionListener;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyCompletionListener implements CompletionListener {

	@Override
	public void onCompletion(Message message) {
		// log.info("Acknowledged");
	}

	@Override
	public void onException(Message message, Exception exception) {
		log.error("Error", exception);
	}
}