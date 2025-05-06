package com.example.messaging.common.manager;

import com.example.messaging.common.task.MessagingTask;
import com.example.messaging.common.task.Task;
import com.example.messaging.common.util.MessagingMetric;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

@Slf4j
public abstract class BaseMessagingManager {

	protected final Map<MessagingMetric, Long> metrics = new HashMap<>();

	protected CountDownLatch startGate;

	protected CountDownLatch endGate;

	protected List<MessagingTask> producerTasks;

	protected List<MessagingTask> consumerTasks;

	protected void setup(int pairs) {
		producerTasks = new ArrayList<>(pairs);
		consumerTasks = new ArrayList<>(pairs);
		startGate = new CountDownLatch(1);
		endGate = new CountDownLatch(pairs * 2);
	}

	protected void start(int pairs) {
		log.info("Starting workers...");
		startProducers(pairs);
		startConsumers(pairs);
		startGate.countDown();
	}

	protected abstract void startProducers(int amount);

	protected abstract void startConsumers(int amount);

	protected void stop() throws InterruptedException {
		log.info("Stopping workers...");
		Stream.concat(producerTasks.stream(), consumerTasks.stream()).forEach(Task::cancel);
		endGate.await();
		printMetrics();
		producerTasks.clear();
		consumerTasks.clear();
	}

	private void printMetrics() {
		collectMetrics();

		for (int i = 0; i < producerTasks.size(); i++) {
			MessagingTask task = producerTasks.get(i);
			log.info("PRODUCER-{} IN: {}", i, task.getInCount());
			log.info("PRODUCER-{} OUT: {}", i, task.getOutCount());
		}

		for (int i = 0; i < consumerTasks.size(); i++) {
			MessagingTask task = consumerTasks.get(i);
			log.info("CONSUMER-{} IN: {}", i, task.getInCount());
			log.info("CONSUMER-{} OUT: {}", i, task.getOutCount());
		}

		metrics.forEach((stat, count) -> log.info("TOTAL {}: {}", stat, count));
	}

	private void collectMetrics() {
		if ((producerTasks != null && !producerTasks.isEmpty()) && (consumerTasks != null && !consumerTasks.isEmpty())) {
			long producerIn = producerTasks.stream().map(MessagingTask::getInCount).reduce(0L, Long::sum);
			long consumerIn = consumerTasks.stream().map(MessagingTask::getInCount).reduce(0L, Long::sum);
			long producerOut = producerTasks.stream().map(MessagingTask::getOutCount).reduce(0L, Long::sum);
			long consumerOut = consumerTasks.stream().map(MessagingTask::getOutCount).reduce(0L, Long::sum);

			metrics.put(MessagingMetric.PRODUCER_IN, producerIn);
			metrics.put(MessagingMetric.CONSUMER_IN, consumerIn);
			metrics.put(MessagingMetric.PRODUCER_OUT, producerOut);
			metrics.put(MessagingMetric.CONSUMER_OUT, consumerOut);
		}
	}

	protected Map<MessagingMetric, Long> getMetrics() {
		collectMetrics();
		return metrics;
	}

	public boolean isProducing() {
		if (producerTasks == null || producerTasks.isEmpty()) {
			return false;
		}

		return producerTasks.stream().anyMatch(MessagingTask::isWorking);
	}

	public boolean isConsuming() {
		if (consumerTasks == null || consumerTasks.isEmpty()) {
			return false;
		}

		return consumerTasks.stream().anyMatch(MessagingTask::isWorking);
	}
}
