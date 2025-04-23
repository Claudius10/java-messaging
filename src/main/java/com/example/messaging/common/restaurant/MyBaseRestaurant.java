package com.example.messaging.common.restaurant;

import com.example.messaging.common.model.Dish;
import com.example.messaging.common.task.MessagingTask;
import com.example.messaging.common.util.DishesStat;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;

@Slf4j
public abstract class MyBaseRestaurant {

	protected List<MessagingTask> chefTasks;

	protected List<MessagingTask> serverTasks;

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
		startProducers(forAmountOfCustomers);
		startConsumers(forAmountOfCustomers);
		startGate.countDown();
	}

	protected void stop() throws InterruptedException {
		log.info("Closing restaurant...");

		List<MessagingTask> allTasks = Stream.concat(chefTasks.stream(), serverTasks.stream()).toList();

		for (MessagingTask task : allTasks) {
			task.cancel();
		}

		endGate.await();
	}

	protected abstract void startProducers(int amount);

	protected abstract void startConsumers(int amount);

	public boolean isProducing() {
		if (chefTasks == null || chefTasks.isEmpty()) return false;
		return chefTasks.stream().anyMatch(MessagingTask::isWorking);
	}

	public boolean isConsuming() {
		if (serverTasks == null || serverTasks.isEmpty()) return false;
		return serverTasks.stream().anyMatch(MessagingTask::isWorking);
	}

	protected Map<DishesStat, Long> getStats() {
		long producerIn = chefTasks.stream().map(MessagingTask::getInCount).reduce(0L, Long::sum);
		long consumerIn = serverTasks.stream().map(MessagingTask::getInCount).reduce(0L, Long::sum);
		long producerOut = chefTasks.stream().map(MessagingTask::getOutCount).reduce(0L, Long::sum);
		long consumerOut = serverTasks.stream().map(MessagingTask::getOutCount).reduce(0L, Long::sum);

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
