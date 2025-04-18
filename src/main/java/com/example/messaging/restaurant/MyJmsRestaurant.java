package com.example.messaging.restaurant;

import com.example.messaging.customer.Customer;
import com.example.messaging.customer.MyCustomer;
import com.example.messaging.jms.JmsConnectionFactory;
import com.example.messaging.jms.producer.JmsProducer;
import com.example.messaging.jms.producer.MyCompletionListener;
import com.example.messaging.jms.producer.MyExceptionListener;
import com.example.messaging.jms.producer.Producer;
import com.example.messaging.task.ChefTask;
import com.example.messaging.task.ServerTask;
import com.example.messaging.util.JmsProperties;
import com.example.messaging.util.RestaurantProperties;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MyJmsRestaurant extends MyBaseRestaurant implements Restaurant {

	private final ThreadPoolTaskScheduler workers;

	private final RestaurantProperties restaurantProperties;

	private final JmsProperties jmsProperties;

	private final JmsConnectionFactory connectionFactory;

	public MyJmsRestaurant(
			ThreadPoolTaskScheduler workers,
			RestaurantProperties restaurantProperties,
			JmsProperties jmsProperties,
			JmsConnectionFactory connectionFactory) {
		super();
		this.workers = workers;
		this.restaurantProperties = restaurantProperties;
		this.jmsProperties = jmsProperties;
		this.connectionFactory = connectionFactory;
	}

	public void open() {
		log.info("Opening restaurant...");
		int maxCustomers = restaurantProperties.getMaxCapacity();
		super.preparations(restaurantProperties.getDishesCapacity(), maxCustomers);
		super.startWork(maxCustomers);
	}

	public void close() {
		super.stop();
		super.doInventory();
		log.info("Restaurant closed");
	}

	@Override
	protected void createConsumers(int amount) {
		for (int i = 0; i < amount; i++) {
			Customer customer = new MyCustomer(restaurantProperties.getDishes());
			ChefTask chefTask = new ChefTask(startGate, dishesQueue, customer, restaurantProperties.getGreetTimeOut());
			chefTasks.add(chefTask);
			runningTasks.add(workers.submit(chefTask));
		}
	}

	@Override
	protected void createProducers(int amount) {
		for (int i = 0; i < amount; i++) {
			Producer jmsProducer = new JmsProducer(connectionFactory.createContext(jmsProperties.getUser(), jmsProperties.getPassword(), Session.AUTO_ACKNOWLEDGE), jmsProperties.getDestination());
			jmsProducer.getContext().setExceptionListener(new MyExceptionListener());
			jmsProducer.getProducer().setAsync(new MyCompletionListener());
			jmsProducer.getProducer().setDeliveryMode(DeliveryMode.PERSISTENT);
			ServerTask serverTask = new ServerTask(startGate, dishesQueue, jmsProducer, restaurantProperties.getTakeGiveUp());
			serverTasks.add(serverTask);
			runningTasks.add(workers.submit(serverTask));
		}
	}
}
