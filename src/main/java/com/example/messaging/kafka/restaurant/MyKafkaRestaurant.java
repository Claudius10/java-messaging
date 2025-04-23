package com.example.messaging.kafka.restaurant;

import com.example.messaging.common.config.RestaurantProperties;
import com.example.messaging.common.customer.Customer;
import com.example.messaging.common.customer.impl.MyCustomer;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.producer.impl.NoopProducer;
import com.example.messaging.common.task.MessagingTask;
import com.example.messaging.common.task.impl.ChefTask;
import com.example.messaging.common.task.impl.ServerTask;
import com.example.messaging.common.util.DishesStat;
import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.producer.impl.MyKafkaProducer;
import com.example.messaging.common.restaurant.MyBaseRestaurant;
import com.example.messaging.common.restaurant.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyKafkaRestaurant extends MyBaseRestaurant implements Restaurant {

	private final ThreadPoolTaskScheduler workers;

	private final RestaurantProperties restaurantProperties;

	private final KafkaProperties kafkaProperties;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	public void open() {
		int maxCustomers = kafkaProperties.getMaxConnections();
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
		if (kafkaProperties.getProducer().equalsIgnoreCase("NoopProducer")) {
			return new NoopProducer();
		}

		return new MyKafkaProducer(kafkaProperties.getTopic(), kafkaTemplate);
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
