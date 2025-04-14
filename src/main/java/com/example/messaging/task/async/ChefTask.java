package com.example.messaging.task.async;

import com.example.messaging.model.Dish;
import com.example.messaging.util.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class ChefTask implements Runnable {

	boolean working = false;

	private final Customer customer;

	private final BlockingQueue<Dish> completedDishes;

	private final int delay;

	@Override
	public void run() {
		working = true;
		log.info("Chef started work");
		work();
	}

	private void work() {
		while (working) {
			try {
				Dish dish = customer.dish();

				if (dish == null) {
					log.info("All dishes cooked. Resting...");
					Thread.sleep(500);
				} else {
					cook(dish);

					boolean result = completedDishes.offer(dish, delay, TimeUnit.SECONDS);

					if (!result) {
						log.error("Dish went cold");
					}
				}
			} catch (InterruptedException ex) {
				log.error("Interrupted", ex);
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
