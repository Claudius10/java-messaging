package com.example.messaging.common.exception.producer;

public class ProducerDeliveryException extends RuntimeException {

	public ProducerDeliveryException(String message) {
		super(message);
	}

	public ProducerDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProducerDeliveryException() {
		super();
	}
}
