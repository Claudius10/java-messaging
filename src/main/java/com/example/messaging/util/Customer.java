package com.example.messaging.util;

import com.example.messaging.exception.CustomerGreetException;
import com.example.messaging.model.Dish;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

@Slf4j
public class Customer {

	private final List<Dish> dishes;

	private final int amountOfDishesToGenerate;

	public Customer(int amountOfDishesToGenerate) {
		dishes = new ArrayList<>();
		this.amountOfDishesToGenerate = amountOfDishesToGenerate;
	}

	public Dish getDish() {
		Dish dish = null;

		try {
			dish = dishes.getFirst();
			dishes.removeFirst();
		} catch (NoSuchElementException ex) {
			// ignore
		}

		return dish;
	}

	// a.k.a sending a Heartbeat
	public void greet() {
		log.info("Getting customer order...");
		if (new Random().nextBoolean()) {
			generateDishes();
		} else {
			throw new CustomerGreetException("Nothing fancy on the menu. Maybe next time.");
		}
	}

	public void generateDishes() {
		if (new Random().nextBoolean()) {
			log.info("Customer ordered {} dishes!", amountOfDishesToGenerate);
			for (long i = 0; i < amountOfDishesToGenerate; i++) {
				Dish dish = Dish.builder().withId(i).withCooked(false).withName("Delicious dish").build();
				dishes.add(dish);
			}
		} else {
			log.info("Customer is satisfied for the time being.");
		}
	}
}
