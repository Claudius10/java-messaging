package com.example.messaging.common.exception.producer;

public class ProducerSendException extends ProducerDeliveryException {

	public ProducerSendException(String message) {
		super(message);
	}

	public ProducerSendException() {
		super();
	}
}
