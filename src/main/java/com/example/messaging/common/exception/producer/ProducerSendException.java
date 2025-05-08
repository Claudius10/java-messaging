package com.example.messaging.common.exception.producer;

public class ProducerSendException extends ProducerDeliveryException {

	public ProducerSendException(String message) {
		super(message);
	}

	public ProducerSendException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProducerSendException() {
		super();
	}
}
