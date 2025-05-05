package com.example.messaging.common.task.restaurant;

import com.example.messaging.common.customer.RestaurantCustomer;
import com.example.messaging.common.exception.customer.CustomerGreetException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.task.MessagingTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

@RequiredArgsConstructor
@Slf4j
public class ChefTask implements MessagingTask {

	private final CountDownLatch startGate;

	private final CountDownLatch endGate;

	private final BlockingQueue<Dish> completedDishes;

	private final RestaurantCustomer customer;

	private final int producerIdle;

	private boolean isWorking = false;

	private boolean cancel = false;

	private long timeOfLastDish = 0;

	private long in = 0;

	private long out = 0;

	@Override
	public void run() {
		startWork();
		log.info("Chef shift ended");
		notifyServers();
	}

	private void startWork() {
		try {

			log.info("Waiting on coworkers...");
			startGate.await();
			log.info("All coworkers ready, starting work!");

			while (!Thread.currentThread().isInterrupted()) {

				if (cancel) {
					isWorking = false;
					endGate.countDown();
					break;
				}

				if (isWorking) {
					notifyServers();
				}

				Dish dish = customer.getDish();

				if (dish != null) {
					try {
						timeOfLastDish = System.currentTimeMillis();
						in++;
						cook(dish);
						out++;
					} catch (IllegalArgumentException | ClassCastException ex) {
						log.warn("Can't add dish {} to queue: {}", dish.getId(), ex.getMessage());
					}
				} else {
					handleIdle();
				}
			}
		} catch (InterruptedException ex) {
			log.warn("Chef interrupted: {}", ex.getMessage());
			isWorking = false;
			Thread.currentThread().interrupt();
		}
	}

	private void cook(Dish dish) throws InterruptedException {
		dish.setCooked(true);
		completedDishes.put(dish);
		if (log.isTraceEnabled()) log.trace("Chef cooked dish {}", dish.getName());
	}

	private void handleIdle() {
		isWorking = false;
		long elapsed = System.currentTimeMillis() - timeOfLastDish;
		if (elapsed > producerIdle) {
			if (log.isTraceEnabled()) log.trace("Elapsed time: {} ms", elapsed);
			if (log.isTraceEnabled()) log.trace("Producer idle: {}", producerIdle);
			greetCustomer();
		}
	}

	private void greetCustomer() {
		if (log.isTraceEnabled()) log.trace("Greeting customer...");
		try {
			customer.greet();
			isWorking = true;
		} catch (CustomerGreetException ex) {
			if (log.isTraceEnabled()) log.trace("Customer response: '{}'", ex.getMessage());
		}
	}

	private void notifyServers() {
		synchronized (completedDishes) {
			completedDishes.notifyAll();
			if (log.isTraceEnabled()) log.trace("Notified servers");
		}
	}

	@Override
	public void cancel() {
		cancel = true;
	}

	@Override
	public boolean isWorking() {
		return isWorking;
	}

	@Override
	public long getInCount() {
		return in;
	}

	@Override
	public long getOutCount() {
		return out;
	}

	@Override
	public long timeInMilisOfLastMessage() {
		return timeOfLastDish;
	}
}
