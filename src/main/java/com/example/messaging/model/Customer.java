package com.example.messaging.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Customer {

	private final Dishes dishes;

	public Customer(int dishesRequested) {
		dishes = new Dishes(dishesRequested);
	}
}
