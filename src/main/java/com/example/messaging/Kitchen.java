package com.example.messaging;

import com.example.messaging.model.Dish;
import com.example.messaging.task.async.ChefTask;
import com.example.messaging.task.async.ServerTask;
import com.example.messaging.util.Customer;
import com.example.messaging.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Kitchen {

	private final Properties properties;

	private final JmsTemplate serverCart;

	private final ThreadPoolTaskExecutor threadPool;

	private final List<Customer> customers = new ArrayList<>();

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void open() {
		log.info("Opening kitchen...");
		//executeMultipleCustomers();
		executeSingleCustomer();
	}

	@Scheduled(initialDelay = 3, fixedRate = 3, timeUnit = TimeUnit.SECONDS)
	public void newOrders() {
		log.info("Refilling dishes...");
		for (Customer customer : customers) {
			customer.refillDishes(properties.getRequestedDishes());
		}
	}

	public void executeSingleCustomer() {
		customers.add(new Customer(properties.getRequestedDishes()));

		ActiveMQQueue diningHall = new ActiveMQQueue("diningHall");

		BlockingQueue<Dish> dishQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());

		int threads = 1;

		for (int i = 0; i < threads; i++) {
			ChefTask chefTask = new ChefTask(customers.getFirst(), dishQueue, properties.getDishesGiveUpDelay());
			ServerTask serverTask = new ServerTask(dishQueue, diningHall, serverCart, properties.getDishesGiveUpDelay());
			threadPool.execute(chefTask);
			threadPool.execute(serverTask);
		}
	}

	public void executeMultipleCustomers() {
		ActiveMQQueue diningHall = new ActiveMQQueue("diningHall");

		for (int i = 0; i < properties.getCustomers(); i++) {
			customers.add(new Customer(properties.getRequestedDishes()));
		}

		BlockingQueue<Dish> dishQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());

		for (Customer customer : customers) {
			threadPool.execute(new ChefTask(customer, dishQueue, properties.getDishesGiveUpDelay()));
			threadPool.execute(new ServerTask(dishQueue, diningHall, serverCart, properties.getDishesGiveUpDelay()));
		}
	}
}