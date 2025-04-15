package com.example.messaging.task.async;

import com.example.messaging.exception.CustomerGreetException;
import com.example.messaging.model.Customer;
import com.example.messaging.model.Dish;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class ChefTask implements Task {

	private final BlockingQueue<Dish> completedDishes;

	private final AtomicInteger counter = new AtomicInteger(0);

	private final Customer customer;

	private final int greetTimeOut;

	private boolean cancel = false;

	private long timeOfLastDish = 0;

	public ChefTask(BlockingQueue<Dish> dishQueue, int amountOfDishesToGenerate, int greetTimeOut) {
		this.completedDishes = dishQueue;
		this.customer = new Customer(amountOfDishesToGenerate);
		this.greetTimeOut = greetTimeOut;
	}

	@Override
	public void run() {
		log.info("Chef started work");
		startWork();
	}

	private void startWork() {
		log.info("Starting work!");
		try {
			while (!Thread.currentThread().isInterrupted()) {

				if (cancel) {
					log.info("Chef shift ended");
					log.info("Chef cooked {} dishes", counter.get());
					break;
				}

				Dish dish = customer.getDish();

				if (dish != null) {
					cook(dish);
					completedDishes.put(dish); // wait: can't have customers go hungry. also, if cancel becomes true, put last dish and end shift
					timeOfLastDish = System.currentTimeMillis();
				} else {
					handleIdle();
				}
			}

		} catch (InterruptedException ex) {
			log.error("Unexpected Interruption", ex);
			Thread.currentThread().interrupt();
		}
	}

	private void cook(Dish dish) {
		dish.setCooked(true);
		log.info("Chef cooked dish {}", dish.getId());
		counter.incrementAndGet();
	}

	private void handleIdle() {
		long now = System.currentTimeMillis();
		long elapsed = now - timeOfLastDish;
		if (elapsed > TimeUnit.SECONDS.toMillis(greetTimeOut)) {
			try {
				customer.greet();
			} catch (CustomerGreetException ex) {
				log.warn("Customer response: '{}'", ex.getMessage());
			}
		}
	}

	@Override
	public void cancel() {
		cancel = true;
	}
}
