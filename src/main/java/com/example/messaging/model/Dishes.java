package com.example.messaging.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Getter
public class Dishes {

	private final List<Dish> dishes;

	public Dishes(int amount) {
		dishes = new ArrayList<>(amount);
		generateDishes(amount);
	}

	public Dish get() {
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
		generateDishes(amount);
	}

	private void generateDishes(int amount) {
		for (long i = 0; i < amount; i++) {
			Dish dish = Dish.builder().withId(i).withCooked(false).build();
			dishes.add(dish);
		}
	}
}
