package com.example.messaging.task.async;

import com.example.messaging.exception.CustomerGreetException;
import com.example.messaging.model.Dish;
import com.example.messaging.util.Customer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ChefTask implements Task {

	private final CountDownLatch startGate;

	private final AtomicInteger in = new AtomicInteger(0);

	private final AtomicInteger out = new AtomicInteger(0);

	private final AtomicBoolean isWorking = new AtomicBoolean(false);

	private final BlockingQueue<Dish> completedDishes;

	private final Customer customer;

	private final int greetTimeOut;

	private boolean cancel = false;

	private long timeOfLastDish = 0;

	public ChefTask(CountDownLatch startGate, BlockingQueue<Dish> completedDishes, int amountOfDishesToGenerate, int greetTimeOut) {
		this.startGate = startGate;
		this.completedDishes = completedDishes;
		this.customer = new Customer(amountOfDishesToGenerate);
		this.greetTimeOut = greetTimeOut;
	}

	@Override
	public void run() {
		startWork();
	}

	private void startWork() {
		try {
			log.info("Waiting on coworkers...");
			startGate.await();
			log.info("All coworkers ready, starting work!");
			while (!Thread.currentThread().isInterrupted()) {

				if (cancel) {
					log.info("Chef shift ended");
					break;
				}

				Dish dish = customer.getDish();

				if (dish != null) {
					try {
						timeOfLastDish = System.currentTimeMillis();
						in.incrementAndGet();
						cook(dish);
						out.incrementAndGet();
					} catch (IllegalArgumentException | ClassCastException ex) {
						log.error("Can't add dish {} to queue: {}", dish.getId(), ex.getMessage());
					}
				} else {
					handleIdle();
				}
			}

		} catch (InterruptedException ex) {
			log.error("Chef interrupted: {}", ex.getMessage());
			isWorking.compareAndSet(true, false);
			Thread.currentThread().interrupt();
		}
	}

	private void cook(Dish dish) throws InterruptedException {
		completedDishes.put(dish); // wait: can't have customers go hungry. also, if cancel becomes true, put last dish and end
	}

	private void handleIdle() {
		long now = System.currentTimeMillis();
		long elapsed = now - timeOfLastDish;
		if (elapsed > TimeUnit.SECONDS.toMillis(greetTimeOut)) {
			try {
				customer.greet();
				isWorking.compareAndSet(false, true);
			} catch (CustomerGreetException ex) {
				isWorking.compareAndSet(true, false);
				log.warn("Customer response: '{}'", ex.getMessage());
			}
		}
	}

	@Override
	public void cancel() {
		cancel = true;
	}

	@Override
	public boolean isWorking() {
		return isWorking.get();
	}

	@Override
	public int getInCount() {
		return in.get();
	}

	@Override
	public int getOutCount() {
		return out.get();
	}
}
