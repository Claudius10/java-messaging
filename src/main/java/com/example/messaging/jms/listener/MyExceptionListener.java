package com.example.messaging.jms.listener;

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyExceptionListener implements ExceptionListener {

	@Override
	public void onException(JMSException exception) {
		log.error("Listener error", exception);
	}
}
