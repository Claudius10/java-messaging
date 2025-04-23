package com.example.messaging.jms.restaurant;

import com.example.messaging.common.customer.Customer;
import com.example.messaging.common.customer.impl.MyCustomer;
import com.example.messaging.jms.config.JmsConnectionFactory;
import com.example.messaging.jms.producer.JmsProducer;
import com.example.messaging.jms.producer.impl.MyJmsProducer;
import com.example.messaging.common.producer.impl.NoopProducer;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.task.impl.ChefTask;
import com.example.messaging.common.task.MessagingTask;
import com.example.messaging.common.task.impl.ServerTask;
import com.example.messaging.common.restaurant.MyBaseRestaurant;
import com.example.messaging.common.restaurant.Restaurant;
import com.example.messaging.common.util.DishesStat;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.common.config.RestaurantProperties;
import jakarta.jms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyJmsRestaurant extends MyBaseRestaurant implements Restaurant {

	private final ThreadPoolTaskScheduler workers;

	private final RestaurantProperties restaurantProperties;

	private final JmsProperties jmsProperties;

	private final JmsConnectionFactory connectionFactory;

	private final ExceptionListener myExceptionListener;

	private final CompletionListener myCompletionListener;

	public void open() {
		int maxCustomers = jmsProperties.getMaxConnections();
		super.preparations(restaurantProperties.getDishesQueueCapacity(), maxCustomers);
		super.startWork(maxCustomers);
	}

	public void close() throws InterruptedException {
		super.stop();
		super.printStats();
	}

	protected void startProducers(int amount) {
		for (int i = 0; i < amount; i++) {
			MessagingTask serverTask = new ServerTask(startGate, endGate, dishesQueue, buildProducer(), restaurantProperties.getTakeGiveUp());
			serverTasks.add(serverTask);
			workers.execute(serverTask);
		}
	}

	protected void startConsumers(int amount) {
		for (int i = 0; i < amount; i++) {
			Customer customer = new MyCustomer(restaurantProperties.getDishesToProduce(), i);
			MessagingTask chefTask = new ChefTask(startGate, endGate, dishesQueue, customer, restaurantProperties.getGreetTimeOut());
			chefTasks.add(chefTask);
			workers.execute(chefTask);
		}
	}

	private Producer buildProducer() {
		if (jmsProperties.getProducer().equalsIgnoreCase("NoopProducer")) {
			return new NoopProducer();
		}

		JmsProducer jmsProducer = new MyJmsProducer(connectionFactory.createContext(jmsProperties.getUser(), jmsProperties.getPassword(), Session.AUTO_ACKNOWLEDGE), jmsProperties.getDestination());
		jmsProducer.getContext().setExceptionListener(myExceptionListener);
		jmsProducer.getProducer().setAsync(myCompletionListener);
		jmsProducer.getProducer().setDeliveryMode(DeliveryMode.PERSISTENT);
		return jmsProducer;
	}

	@Override
	public boolean isCooking() {
		return super.isProducing();
	}

	@Override
	public boolean isServing() {
		return super.isConsuming();
	}

	@Override
	public Map<DishesStat, Long> getStats() {
		return super.getStats();
	}
}
