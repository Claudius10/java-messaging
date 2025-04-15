package com.example.messaging;

import com.example.messaging.model.Dish;
import com.example.messaging.task.async.ChefTask;
import com.example.messaging.task.async.ServerTask;
import com.example.messaging.task.async.Task;
import com.example.messaging.util.Properties;
import jakarta.jms.Destination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Restaurant {

	private final ConfigurableApplicationContext restaurant;

	private final Properties properties;

	private final ThreadPoolTaskScheduler workers;

	private final JmsTemplate serverCart;

	private List<Task> tasks;

	private List<Future<?>> runningTasks;

	private Destination table;

	private BlockingQueue<Dish> dishesQueue;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void open() {
		log.info("Opening restaurant...");
		int maxCustomers = properties.getCustomerMaxCapacity();
		preparations(properties.getDishesCapacity(), properties.getDestination(), maxCustomers);
		for (int i = 0; i < maxCustomers; i++) {
			startWork();
		}
	}

	private void preparations(int dishesCapacity, String destination, int maxCustomers) {
		log.info("Preparing restaurant...");
		dishesQueue = new ArrayBlockingQueue<>(dishesCapacity);

		if (destination.contains("queue")) {
			table = new ActiveMQQueue(destination);
		} else {
			table = new ActiveMQTopic(destination);
		}

		tasks = new ArrayList<>(maxCustomers);
		runningTasks = new ArrayList<>(maxCustomers);
	}

	private void startWork() {
		ChefTask chefTask = new ChefTask(dishesQueue, properties.getRequestedDishes(), properties.getCustomerGreetTimeOut());
		ServerTask serverTask = new ServerTask(dishesQueue, table, serverCart, properties.getPollTimeOut());
		tasks.add(chefTask);
		tasks.add(serverTask);
		runningTasks.add(workers.submit(chefTask));
		runningTasks.add(workers.submit(serverTask));
	}

	@Scheduled(initialDelay = 30, timeUnit = TimeUnit.SECONDS)
	public void close() {
		stop();
		restaurant.close();
		log.info("Restaurant closed");
	}

	public void stop() {
		log.info("Closing restaurant...");

		// cancel all tasks
		for (Task task : tasks) {
			task.cancel();
		}

		// wait for remaining tasks to complete (blocks thread until all tasks complete)
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
}