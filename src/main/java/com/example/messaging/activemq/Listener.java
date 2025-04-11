package com.example.messaging.activemq;


public interface Listener {

	void receive(String content);
}
