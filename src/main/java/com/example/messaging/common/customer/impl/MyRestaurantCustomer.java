package com.example.messaging.common.customer.impl;

import com.example.messaging.common.customer.RestaurantCustomer;
import com.example.messaging.common.exception.customer.CustomerGreetException;
import com.example.messaging.common.model.Dish;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

@Slf4j
public class MyRestaurantCustomer implements RestaurantCustomer {

	private final int amountOfDishesToGenerate;

	private final int id;

	private List<Dish> dishes;

	public MyRestaurantCustomer(int amountOfDishesToGenerate, int id) {
		dishes = new ArrayList<>(amountOfDishesToGenerate);
		this.amountOfDishesToGenerate = amountOfDishesToGenerate;
		this.id = id;
	}

	@Override
	public Dish getDish() {
		Dish dish = null;

		try {
			if (!dishes.isEmpty()) {
				dish = dishes.removeFirst();
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
			dishes = new ArrayList<>();

			if (log.isTraceEnabled()) log.trace("Customer ordered {} dishes!", amountOfDishesToGenerate);

			for (long i = 0; i < amountOfDishesToGenerate; i++) {
				Dish dish = Dish.builder().withId(i).withCooked(false).withName("p-" + id + " Delicious dish " + i).build();
				dishes.add(dish);
			}
		} else {
			if (log.isTraceEnabled()) log.trace("Customer is satisfied for the time being.");
		}
	}
}
