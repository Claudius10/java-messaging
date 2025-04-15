package com.example.messaging;

import com.example.messaging.model.Customer;
import com.example.messaging.model.Dish;
import com.example.messaging.model.Dishes;
import com.example.messaging.task.async.*;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Restaurant {

	private final ConfigurableApplicationContext restaurant;

	private final Properties properties;

	private final List<Customer> customers;

	private final ThreadPoolTaskScheduler workers;

	private final List<Task> tasks = new ArrayList<>();

	private final List<Future<?>> runningTasks = new ArrayList<>();

	private final JmsTemplate serverCart;

	private Destination table;

	private BlockingQueue<Dish> dishesQueue;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void open() {
		log.info("Opening restaurant...");
		preparations();
		searchCustomers(properties.getAmountOfCustomers());
		getCustomerOrders();
		for (Customer customer : customers) {
			startWork(customer.getDishes());
		}
		workers.scheduleAtFixedRate(this::getCustomerOrders, Instant.now().plusSeconds(5), Duration.ofSeconds(5));
	}

	@Scheduled(initialDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void close() {
		stop();
		restaurant.close();
		log.info("Restaurant closed");
	}

	private void preparations() {
		log.info("Preparing restaurant...");

		dishesQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());

		String destination = properties.getDestination();

		if (destination.contains("queue")) {
			table = new ActiveMQQueue(properties.getDestination());
		} else {
			table = new ActiveMQTopic(destination);
		}
	}

	private void searchCustomers(int amount) {
		log.info("Searching for {} customers...", amount);

		for (int i = 0; i < amount; i++) {
			try {
				GreetCustomerTask greetCustomerTask = new GreetCustomerTask();
				greetCustomerTask.start();
				customers.add(greetCustomerTask.greetCustomer());
			} catch (InterruptedException ex) {
				log.error("Interrupted while greeting customers", ex);
			} catch (CustomerGreetException ex) {
				log.warn("Customer response: {}", ex.getMessage());
			}
		}

		log.info("Found {} customers", customers.size());
	}

	private void startWork(Dishes dishes) {
		log.info("Starting work!");
		ChefTask chefTask = new ChefTask(dishesQueue, dishes);
		ServerTask serverTask = new ServerTask(dishesQueue, table, serverCart, properties.getPollTimeOut());
		tasks.add(chefTask);
		tasks.add(serverTask);
		runningTasks.add(workers.submit(chefTask));
		runningTasks.add(workers.submit(serverTask));
	}

	private void getCustomerOrders() {
		log.info("Asking customers for their orders...");

		for (Customer customer : customers) {
			customer.getDishes().generateDishes(properties.getRequestedDishes());
		}
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