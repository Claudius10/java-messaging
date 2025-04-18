package com.example.messaging.restaurant;

import com.example.messaging.customer.Customer;
import com.example.messaging.customer.MyCustomer;
import com.example.messaging.jms.JmsConnectionFactory;
import com.example.messaging.jms.producer.JmsProducer;
import com.example.messaging.jms.producer.Producer;
import com.example.messaging.task.ChefTask;
import com.example.messaging.task.ServerTask;
import com.example.messaging.util.JmsProperties;
import com.example.messaging.util.RestaurantProperties;
import jakarta.jms.CompletionListener;
import jakarta.jms.DeliveryMode;
import jakarta.jms.ExceptionListener;
import jakarta.jms.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MyJmsRestaurant extends MyBaseRestaurant implements Restaurant {

	private final ThreadPoolTaskScheduler workers;

	private final RestaurantProperties restaurantProperties;

	private final JmsProperties jmsProperties;

	private final JmsConnectionFactory connectionFactory;

	private final ExceptionListener myExceptionListener;

	private final CompletionListener myCompletionListener;

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
			jmsProducer.getContext().setExceptionListener(myExceptionListener);
			jmsProducer.getProducer().setAsync(myCompletionListener);
			jmsProducer.getProducer().setDeliveryMode(DeliveryMode.PERSISTENT);
			ServerTask serverTask = new ServerTask(startGate, dishesQueue, jmsProducer, restaurantProperties.getTakeGiveUp());
			serverTasks.add(serverTask);
			runningTasks.add(workers.submit(serverTask));
		}
	}
}
