package com.example.messaging.common.task.restaurant;

import com.example.messaging.common.customer.RestaurantCustomer;
import com.example.messaging.common.exception.customer.CustomerGreetException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.task.MessagingTask;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

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

	public ChefTask(CountDownLatch startGate, CountDownLatch endGate, BlockingQueue<Dish> completedDishes, RestaurantCustomer customer, int producerIdle) {
		this.startGate = startGate;
		this.endGate = endGate;
		this.completedDishes = completedDishes;
		this.customer = customer;
		this.producerIdle = producerIdle;
	}

	@Override
	public void run() {
		startWork();
		log.info("Chef shift ended");
	}

	private void startWork() {
		try {

			log.info("Waiting on coworkers...");
			startGate.await();
			log.info("All coworkers ready, starting work!");

			while (!Thread.currentThread().isInterrupted()) {

				if (cancel) {
					stopWork();
					break;
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
			notifyConsumers();
		} catch (CustomerGreetException ex) {
			isWorking = false;
			if (log.isTraceEnabled()) log.trace("Customer response: '{}'", ex.getMessage());
		}
	}

	private void notifyConsumers() {
		synchronized (completedDishes) {
			completedDishes.notifyAll();
			if (log.isTraceEnabled()) log.trace("Notified servers");
		}
	}

	private void stopWork() {
		notifyConsumers();
		isWorking = false;
		endGate.countDown();
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
