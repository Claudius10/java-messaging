package com.example.messaging.task.async;

import com.example.messaging.model.Dish;
import com.example.messaging.model.Customer;
import com.example.messaging.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
@Slf4j
public class Manager implements Runnable {

	private final Properties properties;

	private final List<Customer> customers;

	private final ThreadPoolTaskScheduler workers;

	private final List<Task> tasks = new ArrayList<>();

	private final List<Future<?>> runningTasks = new ArrayList<>();

	private ActiveMQDestination table;

	private BlockingQueue<Dish> orderQueue;

	private final JmsTemplate serverCart;

	@Override
	public void run() {
		preparations();
		greetCustomers();
		startWork();
		workers.scheduleAtFixedRate(this::getCustomerOrders, Instant.now().plusSeconds(10), Duration.ofSeconds(10));
	}

	private void preparations() {
		log.info("Preparing");

		orderQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());

		String destination = properties.getDestination();
		if (destination.contains("queue")) {
			table = new ActiveMQQueue(properties.getDestination());
		} else {
			table = new ActiveMQTopic(destination);
		}
	}

	private void greetCustomers() {
		log.info("Greeting Customers");

		for (int i = 0; i < properties.getCustomers(); i++) {
			customers.add(new Customer(properties.getRequestedDishes()));
		}
	}

	private void startWork() {
		log.info("Starting work");

		for (Customer customer : customers) {
			ChefTask chefTask = new ChefTask(orderQueue, customer.getDishes(), properties.getDishesGiveUpDelay());
			ServerTask serverTask = new ServerTask(orderQueue, table, serverCart, properties.getDishesGiveUpDelay());
			tasks.add(chefTask);
			tasks.add(serverTask);
			runningTasks.add(workers.submit(chefTask));
			runningTasks.add(workers.submit(serverTask));
		}
	}

	private void getCustomerOrders() {
		log.info("Getting customer orders");

		for (Customer customer : customers) {
			customer.getDishes().refillDishes(properties.getRequestedDishes());
		}
	}

	public void stop() {
		log.info("Stopping");

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

		// stop work
		Thread.currentThread().interrupt();
	}
}
