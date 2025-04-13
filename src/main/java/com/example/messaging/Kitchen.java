package com.example.messaging;

import com.example.messaging.activemq.consumer.ServerTask;
import com.example.messaging.activemq.producer.ChefTask;
import com.example.messaging.model.Dish;
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
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Kitchen {

	private final Properties properties;

	private final JmsTemplate serverCart;

	private final ThreadPoolTaskExecutor chefs;

	private final ThreadPoolTaskExecutor servers;

	private final ConfigurableApplicationContext context;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void execute() {
		//executeMultipleCustomers();
		executeSingleCustomer();
	}

	public void executeMultipleCustomers() {
		ActiveMQQueue diningHall = new ActiveMQQueue("diningHall");

		List<Customer> customers = new ArrayList<>();

		for (int i = 0; i < properties.getCustomers(); i++) {
			customers.add(new Customer(properties.getRequestedDishes()));
		}

		List<Future<?>> tasks = new ArrayList<>();

		StopWatch stopWatch = new StopWatch("Kitchen Tasks");

		stopWatch.start();

		for (Customer customer : customers) {
			BlockingQueue<Dish> dishQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());
			tasks.add(chefs.submit(new ChefTask(customer, dishQueue)));
			tasks.add(servers.submit(new ServerTask(dishQueue, diningHall, serverCart, properties.getDishesGiveUpDelay())));
		}


		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		log.info("All done");
		stopWatch.stop();
		log.info(stopWatch.prettyPrint());
		context.close();
	}

	public void executeSingleCustomer() {
		ActiveMQQueue diningHall = new ActiveMQQueue("diningHall");

		Customer customer = new Customer(properties.getRequestedDishes());
		BlockingQueue<Dish> dishQueue = new ArrayBlockingQueue<>(properties.getDishesCapacity());
		List<Future<?>> tasks = new ArrayList<>();
		StopWatch stopWatch = new StopWatch("Kitchen Tasks");

		int chefs = 1;
		int servers = 1;

		stopWatch.start();

		for (int i = 0; i < chefs; i++) {
			tasks.add(this.chefs.submit(new ChefTask(customer, dishQueue)));
		}

		for (int i = 0; i < servers; i++) {
			tasks.add(this.servers.submit(new ServerTask(dishQueue, diningHall, serverCart, properties.getDishesGiveUpDelay())));
		}

		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		log.info("All done");
		stopWatch.stop();
		log.info(stopWatch.prettyPrint());
		context.close();
	}
}