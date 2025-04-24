package com.example.messaging.common.producer;

import com.example.messaging.common.exception.ProducerDeliveryException;

public interface Producer {

	void close();

	void sendTextMessage(long id, String content) throws ProducerDeliveryException;
}
