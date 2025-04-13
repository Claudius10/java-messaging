package com.example.messaging.activemq;

import com.example.messaging.model.Message;
import jakarta.jms.Destination;

public interface Publisher {

	void publishMessage(Message message);

	void setDestination(Destination destination);
}
