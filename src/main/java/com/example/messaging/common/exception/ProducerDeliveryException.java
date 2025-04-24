package com.example.messaging.common.exception;

public class ProducerDeliveryException extends RuntimeException {
	public ProducerDeliveryException(String message) {
		super(message);
	}
}
