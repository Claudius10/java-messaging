package com.example.messaging;

import com.example.messaging.exception.MyExceptionListener;
import com.example.messaging.task.async.ChefTask;
import com.example.messaging.task.async.ServerTask;
import com.example.messaging.task.async.Task;
import com.example.messaging.util.JmsConnection;
import com.example.messaging.util.JmsProperties;
import com.example.messaging.util.Properties;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class Restaurant {

	private final ConfigurableApplicationContext restaurant;

	private final Properties properties;

	private final JmsProperties jmsProperties;

	private final ThreadPoolTaskScheduler workers;

	private final ConnectionFactory jmsPoolConnectionFactory;

//	private final JmsTemplate jmsTemplate;

	private List<Task> chefTasks;

	private List<Task> serverTasks;

	private List<Future<?>> runningTasks; // as an interrupt mechanism

	private BlockingQueue<Long> dishesQueue;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void open() {
		log.info("Opening restaurant...");
		int maxCustomers = properties.getCustomerMaxCapacity();
		preparations(properties.getDishesCapacity(), maxCustomers);
		startWork(maxCustomers);
	}

	@Scheduled(initialDelay = 10, timeUnit = TimeUnit.SECONDS)
	public void close() {
		stop();
		printStats();
		restaurant.close();
		log.info("Restaurant closed");
	}

	public void preparations(int dishesCapacity, int maxCustomers) {
		log.info("Preparing restaurant...");
		dishesQueue = new LinkedBlockingDeque<>(dishesCapacity);
		chefTasks = new ArrayList<>(maxCustomers);
		serverTasks = new ArrayList<>(maxCustomers);
		runningTasks = new ArrayList<>(maxCustomers);
	}

	public void startWork(int forAmountOfCustomers) {

		final CountDownLatch startGate = new CountDownLatch(1);

		for (int i = 0; i < forAmountOfCustomers; i++) {
			ChefTask chefTask = new ChefTask(startGate, dishesQueue, properties.getRequestedDishes(), properties.getCustomerGreetTimeOut());
			chefTasks.add(chefTask);
			runningTasks.add(workers.submit(chefTask));

			JmsConnection jmsConnection = new JmsConnection();
			jmsConnection.connect(jmsPoolConnectionFactory, new MyExceptionListener(), jmsProperties.getDestination());

			ServerTask serverTask = new ServerTask(startGate, dishesQueue, jmsConnection, properties.getPollTimeOut());
			serverTasks.add(serverTask);
			runningTasks.add(workers.submit(serverTask));
		}

		// begin
		startGate.countDown();
	}

	public void stop() {
		log.info("Closing restaurant...");

		List<Task> allTasks = Stream.concat(chefTasks.stream(), serverTasks.stream()).toList();

		for (Task task : allTasks) {
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

	public void printStats() {
		int cookedInDishes = chefTasks.stream().map(Task::getInCount).reduce(0, Integer::sum);
		int servedInDishes = serverTasks.stream().map(Task::getInCount).reduce(0, Integer::sum);
		int cookedOutDishes = chefTasks.stream().map(Task::getOutCount).reduce(0, Integer::sum);
		int servedOutDishes = serverTasks.stream().map(Task::getOutCount).reduce(0, Integer::sum);

		log.info("In Cooked {} dishes", cookedInDishes);
		log.info("Out Cooked {} dishes", cookedOutDishes);
		log.info("In Served {} dishes", servedInDishes);
		log.info("Out Served {} dishes", servedOutDishes);
	}
}