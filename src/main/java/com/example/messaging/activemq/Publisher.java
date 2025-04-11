package com.example.messaging.activemq;

import com.example.messaging.model.Message;

public interface Publisher {

	void publishMessage(Message message);
}
