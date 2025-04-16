package com.example.messaging.config;

import jakarta.jms.CompletionListener;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyCompletionListener implements CompletionListener {
	@Override
	public void onCompletion(Message message) {

	}

	@Override
	public void onException(Message message, Exception exception) {
		log.error("Error", exception);
	}
}