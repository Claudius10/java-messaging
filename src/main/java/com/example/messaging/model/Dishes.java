package com.example.messaging.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Getter
public class Dishes {

	private final List<Dish> dishes;

	public Dishes() {
		dishes = new ArrayList<>();
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

	public void generateDishes(int amount) {
		for (long i = 0; i < amount; i++) {
			Dish dish = Dish.builder().withId(i).withCooked(false).build();
			dishes.add(dish);
		}
	}
}
