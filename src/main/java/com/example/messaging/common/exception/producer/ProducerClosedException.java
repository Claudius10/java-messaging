package com.example.messaging.common.exception.producer;

public class ProducerClosedException extends ProducerDeliveryException {

	public ProducerClosedException(String message) {
		super(message);
	}

	public ProducerClosedException() {
		super();
	}
}
