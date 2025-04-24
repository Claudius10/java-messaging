package com.example.messaging.kafka.restaurant;

import com.example.messaging.common.customer.RestaurantCustomer;
import com.example.messaging.common.customer.impl.MyRestaurantCustomer;
import com.example.messaging.common.manager.BaseMessagingManager;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.producer.backup.BackupProducer;
import com.example.messaging.common.producer.impl.NoopProducer;
import com.example.messaging.common.task.MessagingTask;
import com.example.messaging.common.task.restaurant.ChefTask;
import com.example.messaging.common.task.restaurant.ServerTask;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.producer.impl.MyKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyKafkaRestaurant extends BaseMessagingManager implements MessagingManager {

	private final ThreadPoolTaskScheduler workers;

	private final RestaurantProperties restaurantProperties;

	private final KafkaProperties kafkaProperties;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final BackupProducer myBackupProducer;

	private BlockingQueue<Dish> dishesQueue;

	public void open() {
		dishesQueue = new LinkedBlockingQueue<>(restaurantProperties.getDishesQueueCapacity());
		int maxCustomers = kafkaProperties.getMaxConnections();
		super.setup(maxCustomers);
		super.start(maxCustomers);
	}

	public void close() throws InterruptedException {
		super.stop();
		super.printStats();
	}

	protected void startProducers(int amount) {
		for (int i = 0; i < amount; i++) {
			MessagingTask serverTask = new ServerTask(startGate, endGate, dishesQueue, buildProducer(), myBackupProducer, restaurantProperties.getTakeGiveUp());
			consumerTasks.add(serverTask);
			workers.execute(serverTask);
		}
	}

	protected void startConsumers(int amount) {
		for (int i = 0; i < amount; i++) {
			RestaurantCustomer customer = new MyRestaurantCustomer(restaurantProperties.getDishesToProduce(), i);
			MessagingTask chefTask = new ChefTask(startGate, endGate, dishesQueue, customer, restaurantProperties.getGreetTimeOut());
			producerTasks.add(chefTask);
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
	public boolean isProducing() {
		return super.isProducing();
	}

	@Override
	public boolean isConsuming() {
		return super.isConsuming();
	}

	@Override
	public Map<MessagingStat, Long> getStats() {
		return super.getStats();
	}
}
