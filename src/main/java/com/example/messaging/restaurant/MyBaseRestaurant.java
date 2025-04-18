package com.example.messaging.restaurant;

import com.example.messaging.model.Dish;
import com.example.messaging.task.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
public abstract class MyBaseRestaurant {

	protected List<Task> chefTasks;

	protected List<Task> serverTasks;

	protected List<Future<?>> runningTasks; // as an interruption mechanism

	protected BlockingQueue<Dish> dishesQueue;

	protected final CountDownLatch startGate = new CountDownLatch(1);

	protected void preparations(int dishesCapacity, int maxCustomers) {
		log.info("Preparing restaurant...");
		dishesQueue = new LinkedBlockingDeque<>(dishesCapacity);
		chefTasks = new ArrayList<>(maxCustomers);
		serverTasks = new ArrayList<>(maxCustomers);
		runningTasks = new ArrayList<>(maxCustomers);
	}

	protected void startWork(int forAmountOfCustomers) {
		createConsumers(forAmountOfCustomers);
		createProducers(forAmountOfCustomers);
		startGate.countDown();
	}

	public void doInventory() {
		int cookedInDishes = chefTasks.stream().map(Task::getInCount).reduce(0, Integer::sum);
		int servedInDishes = serverTasks.stream().map(Task::getInCount).reduce(0, Integer::sum);
		int cookedOutDishes = chefTasks.stream().map(Task::getOutCount).reduce(0, Integer::sum);
		int servedOutDishes = serverTasks.stream().map(Task::getOutCount).reduce(0, Integer::sum);

		log.info("In Cooked {} dishes", cookedInDishes);
		log.info("Out Cooked {} dishes", cookedOutDishes);
		log.info("In Served {} dishes", servedInDishes);
		log.info("Out Served {} dishes", servedOutDishes);
	}

	public void stop() {
		log.info("Closing restaurant...");

		List<Task> allTasks = Stream.concat(chefTasks.stream(), serverTasks.stream()).toList();

		for (Task task : allTasks) {
			task.cancel();
		}

		// block thread until all tasks complete
		for (Future<?> kitchenTask : runningTasks) {
			try {
				kitchenTask.get();
			} catch (ExecutionException ex) {
				log.error("Error while executing task", ex);
			} catch (InterruptedException ex) {
				log.error("Task was interrupted", ex);
			}
		}
	}

	protected abstract void createConsumers(int amount);

	protected abstract void createProducers(int amount);
}
