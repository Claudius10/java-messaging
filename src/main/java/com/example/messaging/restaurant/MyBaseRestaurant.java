package com.example.messaging.restaurant;

import com.example.messaging.model.Dish;
import com.example.messaging.task.Task;
import com.example.messaging.util.DishesStat;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
public abstract class MyBaseRestaurant {

	protected List<Task> chefTasks;

	protected List<Task> serverTasks;

	protected BlockingQueue<Dish> dishesQueue;

	protected final CountDownLatch startGate = new CountDownLatch(1);

	protected CountDownLatch endGate;

	protected void preparations(int dishesCapacity, int maxCustomers) {
		log.info("Preparing restaurant...");
		dishesQueue = new LinkedBlockingDeque<>(dishesCapacity);
		chefTasks = new ArrayList<>(maxCustomers);
		serverTasks = new ArrayList<>(maxCustomers);
		endGate = new CountDownLatch(maxCustomers * 2);
	}

	protected void startWork(int forAmountOfCustomers) {
		createConsumers(forAmountOfCustomers);
		createProducers(forAmountOfCustomers);
		startGate.countDown();
	}

	protected void stop() throws InterruptedException {
		log.info("Closing restaurant...");

		List<Task> allTasks = Stream.concat(chefTasks.stream(), serverTasks.stream()).toList();

		for (Task task : allTasks) {
			task.cancel();
		}

		endGate.await();
	}

	protected abstract void createConsumers(int amount);

	protected abstract void createProducers(int amount);

	protected Map<DishesStat, Long> getStats() {
		long producerIn = chefTasks.stream().map(Task::getInCount).reduce(0L, Long::sum);
		long consumerIn = serverTasks.stream().map(Task::getInCount).reduce(0L, Long::sum);
		long producerOut = chefTasks.stream().map(Task::getOutCount).reduce(0L, Long::sum);
		long consumerOut = serverTasks.stream().map(Task::getOutCount).reduce(0L, Long::sum);

		Map<DishesStat, Long> stats = new HashMap<>();

		stats.put(DishesStat.PRODUCER_IN, producerIn);
		stats.put(DishesStat.CONSUMER_IN, consumerIn);
		stats.put(DishesStat.PRODUCER_OUT, producerOut);
		stats.put(DishesStat.CONSUMER_OUT, consumerOut);

		return stats;
	}

	protected void printStats() {
		getStats().forEach((stat, count) -> {
			log.info("{} - {} dishes", stat, count);
		});
	}
}
