package com.example.messaging;

import com.example.messaging.jmsTwo.JmsConnectionFactory;
import com.example.messaging.model.Dish;
import com.example.messaging.producer.JmsProducer;
import com.example.messaging.producer.MyCompletionListener;
import com.example.messaging.producer.MyExceptionListener;
import com.example.messaging.producer.Producer;
import com.example.messaging.task.async.ChefTask;
import com.example.messaging.task.async.ServerTask;
import com.example.messaging.task.async.Task;
import com.example.messaging.util.JmsProperties;
import com.example.messaging.util.Properties;
import jakarta.annotation.PostConstruct;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Session;
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

	private final JmsConnectionFactory connectionFactory;

	private List<Task> chefTasks;

	private List<Task> serverTasks;

	private List<Future<?>> runningTasks; // as an interruption mechanism

	private BlockingQueue<Dish> dishesQueue;

	private final CountDownLatch startGate = new CountDownLatch(1);

	@PostConstruct
	public void open() {
		log.info("Opening restaurant...");
		int maxCustomers = properties.getCustomerMaxCapacity();
		preparations(properties.getDishesCapacity(), maxCustomers);
		startWork(maxCustomers);
	}

	@Scheduled(initialDelay = 30, timeUnit = TimeUnit.SECONDS)
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
		createConsumers(forAmountOfCustomers);
		createProducers(forAmountOfCustomers);
		startGate.countDown();
	}

	private void createConsumers(int amount) {
		for (int i = 0; i < amount; i++) {
			ChefTask chefTask = new ChefTask(startGate, dishesQueue, properties.getRequestedDishes(), properties.getCustomerGreetTimeOut());
			chefTasks.add(chefTask);
			runningTasks.add(workers.submit(chefTask));
		}
	}

	private void createProducers(int amount) {
		for (int i = 0; i < amount; i++) {
			Producer jmsProducer = new JmsProducer(connectionFactory.createContext(jmsProperties.getUser(), jmsProperties.getPassword(), Session.AUTO_ACKNOWLEDGE), jmsProperties.getDestination());
			jmsProducer.getContext().setExceptionListener(new MyExceptionListener());
			jmsProducer.getProducer().setAsync(new MyCompletionListener());
			jmsProducer.getProducer().setDeliveryMode(DeliveryMode.PERSISTENT);
			ServerTask serverTask = new ServerTask(startGate, dishesQueue, jmsProducer, properties.getPollTimeOut());
			serverTasks.add(serverTask);
			runningTasks.add(workers.submit(serverTask));
		}
	}

	public void stop() {
		log.info("Closing restaurant...");

		List<Task> allTasks = Stream.concat(chefTasks.stream(), serverTasks.stream()).toList();

		for (Task task : allTasks) {
			task.cancel();
		}

		// block thread until all tasks complete
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