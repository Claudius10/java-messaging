package com.example.messaging;

import com.example.messaging.model.Dish;
import com.example.messaging.task.async.ChefTask;
import com.example.messaging.task.async.ServerTask;
import com.example.messaging.task.async.Task;
import com.example.messaging.util.Customer;
import com.example.messaging.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Kitchen {

	private final Properties properties;

	private final JmsTemplate serverCart;

	private final ThreadPoolTaskExecutor threadPool;

	private final List<Customer> customers = new ArrayList<>();

	private final List<Task> tasks = new ArrayList<>();

	private final List<Future<?>> kitchenTasks = new ArrayList<>();

	private final ConfigurableApplicationContext context;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void open() {
		log.info("Opening kitchen...");
		//executeMultipleCustomers();
		executeSingleCustomer();
	}

	@Scheduled(initialDelay = 3, fixedRate = 25, timeUnit = TimeUnit.SECONDS)
	public void newOrders() {
		log.info("Refilling dishes...");
		for (Customer customer : customers) {
			customer.refillDishes(properties.getRequestedDishes());
		}
	}

	@Scheduled(initialDelay = 60, timeUnit = TimeUnit.SECONDS)
	public void close() throws ExecutionException, InterruptedException {
		log.info("Closing kitchen...");

		// cancel all tasks
		for (Task task : tasks) {
			task.cancel();
		}

		// wait for remaining tasks to complete (blocks thread until all tasks complete)
		for (Future<?> kitchenTask : kitchenTasks) {
			kitchenTask.get();
		}

		// shutdown
		context.close();
		log.info("Kitchen closed");
	}

	public void executeSingleCustomer() {
		customers.add(new Customer(properties.getRequestedDishes()));

		ActiveMQQueue diningHall = new ActiveMQQueue("diningHall");

		BlockingQueue<Dish> dishQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());

		int threads = 4;

		for (int i = 0; i < threads; i++) {
			ChefTask chefTask = new ChefTask(dishQueue, customers.getFirst(), properties.getDishesGiveUpDelay());
			ServerTask serverTask = new ServerTask(dishQueue, diningHall, serverCart, properties.getDishesGiveUpDelay());
			tasks.add(chefTask);
			tasks.add(serverTask);
			kitchenTasks.add(threadPool.submit(chefTask));
			kitchenTasks.add(threadPool.submit(serverTask));
		}
	}

	public void executeMultipleCustomers() {
		ActiveMQQueue diningHall = new ActiveMQQueue("diningHall");

		for (int i = 0; i < properties.getCustomers(); i++) {
			customers.add(new Customer(properties.getRequestedDishes()));
		}

		BlockingQueue<Dish> dishQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());

		for (Customer customer : customers) {
			threadPool.submit(new ChefTask(dishQueue, customers.getFirst(), properties.getDishesGiveUpDelay()));
			threadPool.submit(new ServerTask(dishQueue, diningHall, serverCart, properties.getDishesGiveUpDelay()));
		}
	}
}