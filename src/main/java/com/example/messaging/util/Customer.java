package com.example.messaging.util;

import com.example.messaging.model.Dish;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
public class Customer {

	private final List<Dish> dishes;

	public Customer(int amount) {
		dishes = new ArrayList<>(requestDishes(amount));
	}

	public Dish dish() {
		Dish dish = null;

		try {
			dish = dishes.getFirst();
			dishes.removeFirst();
		} catch (NoSuchElementException ex) {
			// ignore
		}

		return dish;
	}

	public void refillDishes(int amount) {
		dishes.addAll(requestDishes(amount));
	}

	private List<Dish> requestDishes(int amount) {
		List<Dish> dishes = new ArrayList<>();

		for (long i = 0; i < amount; i++) {
			Dish dish = Dish.builder()
					.withId(i)
					.build();
			dishes.add(dish);
		}

		return dishes;
	}
}
