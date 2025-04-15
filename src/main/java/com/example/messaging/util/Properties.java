package com.example.messaging.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Properties {

	@Value("${dishes}")
	private int requestedDishes;

	@Value("${dishes.capacity}")
	private int dishesCapacity;

	@Value("${dishes.giveUp}")
	private int pollTimeOut;

	@Value("${customers}")
	private int amountOfCustomers;

	@Value("${destination}")
	private String destination;
}
