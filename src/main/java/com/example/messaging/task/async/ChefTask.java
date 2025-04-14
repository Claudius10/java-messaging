package com.example.messaging.task.async;

import com.example.messaging.model.Dish;
import com.example.messaging.util.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class ChefTask implements Task {

	private final BlockingQueue<Dish> completedDishes;

	private final AtomicInteger counter = new AtomicInteger(0);

	private final Customer customer;

	private final int delay;

	private boolean working = true;

	private boolean cancel = false;

	@Override
	public Boolean call() {
		log.info("Chef started work");
		return work();
	}

	private boolean work() {
		try {
			while (working) {

				if (cancel) {
					stop();
					return working;
				}

				Dish dish = customer.dish();

				if (dish == null) {


				} else {
					cook(dish);

					boolean result = completedDishes.offer(dish, delay, TimeUnit.SECONDS);

					if (!result) {
						log.error("Dish went cold");
					}
				}
			}

		} catch (InterruptedException ex) {
			log.error("Interrupted", ex);
			Thread.currentThread().interrupt();
		}

		return working;
	}

	private void cook(Dish dish) {
		dish.setCooked(true);
		log.info("Chef cooked dish {}", dish.getId());
		counter.incrementAndGet();
	}

	@Override
	public void cancel() {
		cancel = true;
		log.info("Chef work cancelled");
	}

	private void stop() {
		working = false;
		log.info("Chef shift ended");
		log.info("Chef cooked {} dishes", counter.get());
	}
}
