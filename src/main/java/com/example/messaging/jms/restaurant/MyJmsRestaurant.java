package com.example.messaging.jms.restaurant;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.customer.impl.MyRestaurantCustomer;
import com.example.messaging.common.manager.BaseMessagingManager;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.producer.impl.NoopProducer;
import com.example.messaging.common.task.MessagingTask;
import com.example.messaging.common.task.restaurant.ChefTask;
import com.example.messaging.common.task.restaurant.ServerTask;
import com.example.messaging.common.util.MessagingMetric;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
import com.example.messaging.jms.producer.impl.MyJmsProducer;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyJmsRestaurant extends BaseMessagingManager implements MessagingManager {

	private final TaskExecutor workers;

	private final RestaurantProperties restaurantProperties;

	private final JmsProperties jmsProperties;

	private final ConnectionFactory connectionFactory;

	private final BackupProvider<Dish> dishBackupProvider;

	private final ProducerMetrics producerMetrics;

	private BlockingQueue<Dish> dishesQueue;

	public void open() {
		dishesQueue = new LinkedBlockingQueue<>(restaurantProperties.getDishesQueueCapacity());
		int maxCustomers = jmsProperties.getMaxConnections();
		super.setup(maxCustomers);
		super.start(maxCustomers);
	}

	public void close() throws InterruptedException {
		super.stop();
	}

	protected void startProducers(int amount) {
		log.info("Starting {} producers", amount);

		for (int i = 0; i < amount; i++) {
			MessagingTask chefTask = new ChefTask(
					startGate,
					endGate,
					dishesQueue,
					new MyRestaurantCustomer(restaurantProperties.getDishesToProduce(), i),
					restaurantProperties.getProducerIdle()
			);

			producerTasks.add(chefTask);
			workers.execute(chefTask);
		}
	}

	protected void startConsumers(int amount) {
		log.info("Starting {} consumers", amount);

		for (int i = 0; i < amount; i++) {
			MessagingTask serverTask = new ServerTask(
					startGate,
					endGate,
					dishesQueue,
					buildProducer(),
					dishBackupProvider,
					restaurantProperties.getConsumerIdle(),
					jmsProperties.getPollTimeOut()
			);

			consumerTasks.add(serverTask);
			workers.execute(serverTask);
		}
	}

	private Producer<Dish> buildProducer() {
		if (jmsProperties.getProducer().equalsIgnoreCase("NoopProducer")) {
			return new NoopProducer();
		}

		return new MyJmsProducer(connectionFactory, jmsProperties, producerMetrics, new MyCompletionListener(dishBackupProvider, producerMetrics));
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
	public Map<MessagingMetric, Long> getMetrics() {
		return super.getMetrics();
	}
}
