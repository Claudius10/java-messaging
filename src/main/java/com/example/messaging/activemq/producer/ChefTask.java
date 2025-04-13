package com.example.messaging.activemq.producer;

import com.example.messaging.model.Dish;
import com.example.messaging.util.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
@Slf4j
public class ChefTask implements Runnable {

	boolean working = false;

	private final Customer customer;

	private final BlockingQueue<Dish> completedDishes;

	@Override
	public void run() {
		working = true;
		log.info("Chef started work");
		work();
	}

	private void work() {
		while (working) {

			Dish dish = customer.dish();

			if (dish == null) {
				stopWork();
			} else {
				cook(dish);
				try {
					completedDishes.put(dish);
				} catch (InterruptedException ex) {
					log.error("Interrupted", ex);
				}
			}
		}
	}

	private void cook(Dish dish) {
		dish.setCooked(true);
		log.info("Chef cooked dish {}", dish.getId());
	}

	public void stopWork() {
		log.info("Chef is resting");
		working = false;
	}
}
