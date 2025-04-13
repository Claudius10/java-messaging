package com.example.messaging.activemq;

import com.example.messaging.model.Dish;
import jakarta.jms.Destination;

public interface Publisher {

	void serve(Dish dish);

	void setDestination(Destination destination);
}
