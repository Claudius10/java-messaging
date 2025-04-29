package com.example.messaging.common.producer;

import com.example.messaging.common.exception.producer.ProducerDeliveryException;

public interface Producer<T> {

	void sendTextMessage(T object) throws ProducerDeliveryException;

	void close();

	boolean isConnected();
}
