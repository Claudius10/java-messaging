package com.example.messaging.contracts;

import com.example.messaging.model.JmsTextMessageDTO;

public interface Publisher {

	void publishMessage(JmsTextMessageDTO message);
}
