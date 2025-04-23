package com.example.messaging.common.producer;

public interface Producer {

	void close();

	void sendTextMessage(long id, String content);
}
