package com.example.messaging.common.customer.impl;

import com.example.messaging.common.customer.Customer;
import com.example.messaging.common.exception.CustomerGreetException;
import com.example.messaging.common.model.Dish;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

@Slf4j
public class MyCustomer implements Customer {

	private List<Dish> dishes;

	private final int amountOfDishesToGenerate;

	private final int id;

	public MyCustomer(int amountOfDishesToGenerate, int id) {
		dishes = new ArrayList<>(amountOfDishesToGenerate);
		this.amountOfDishesToGenerate = amountOfDishesToGenerate;
		this.id = id;
	}

	@Override
	public Dish getDish() {
		Dish dish = null;

		try {
			if (!dishes.isEmpty()) {
				dish = dishes.getFirst();
				dishes.removeFirst();
			}
		} catch (NoSuchElementException ex) {
			dishes = null;
		}

		return dish;
	}

	@Override
	public void greet() {
		if (new Random().nextBoolean()) {
			generateDishes();
		} else {
			throw new CustomerGreetException("Nothing fancy on the menu. Maybe next time.");
		}
	}

	private void generateDishes() {
		if (new Random().nextBoolean()) {
			if (log.isTraceEnabled()) log.trace("Customer ordered {} dishes!", amountOfDishesToGenerate);
			dishes = new ArrayList<>();
			for (long i = 0; i < amountOfDishesToGenerate; i++) {
				Dish dish = Dish.builder().withId(i).withCooked(false).withName("Delicious dish " + id + ":" + i).build();
				dishes.add(dish);
			}
		} else {
			if (log.isTraceEnabled()) log.trace("Customer is satisfied for the time being.");
		}
	}
}
