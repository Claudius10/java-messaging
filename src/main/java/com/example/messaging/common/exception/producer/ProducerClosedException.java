package com.example.messaging.common.exception.producer;

public class ProducerClosedException extends ProducerDeliveryException {

	public ProducerClosedException(String message) {
		super(message);
	}

	public ProducerClosedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProducerClosedException() {
		super();
	}
}
