package com.example.messaging.jms.restaurant;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsProperties;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Slf4j
public class MyJmsRestaurantSafetyTests {

	@Test
	void givenNDishes_thenServerAndCookNDishes() throws InterruptedException {

		int pairs = 3;
		int blockingQueueCapacity = 100;
		int trials = 10000;
		int trialDurationMilis = 100;
		int dishesToProduce = 10000;

		testRestaurantSafety(trials, blockingQueueCapacity, pairs, trialDurationMilis, dishesToProduce);
	}

	void testRestaurantSafety(int trials, int capacity, int pairs, int duration, int amount) throws InterruptedException {
		ThreadPoolTaskScheduler workers = workers(pairs);

		for (int i = 0; i < trials; i++) {
			restaurantTest(workers, capacity, pairs, duration, amount);
			log.info("Trial {} OK", i);
		}
	}

	void restaurantTest(ThreadPoolTaskScheduler workers, int capacity, int pairs, int duration, int amount) throws InterruptedException {
		// Arrange

		RestaurantProperties restaurantProperties = new RestaurantProperties();
		restaurantProperties.setDishesToProduce(amount);
		restaurantProperties.setDishesQueueCapacity(capacity);
		restaurantProperties.setConsumerIdle(999999);
		restaurantProperties.setProducerIdle(999999);

		JmsProperties jmsProperties = new JmsProperties();
		jmsProperties.setUser("user");
		jmsProperties.setPassword("password");
		jmsProperties.setDestination("queue");
		jmsProperties.setProducer("NoopProducer");
		jmsProperties.setMaxConnections(pairs);
		jmsProperties.setPollTimeOut(2);
		jmsProperties.setConsumerClientId("consumer");
		jmsProperties.setReconnectionIntervalMs(5000);

		ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
		BackupProvider backupProvider = mock(BackupProvider.class);

		MessagingManager myJmsRestaurant = new MyJmsRestaurant(
				workers,
				restaurantProperties,
				jmsProperties,
				jmsConnectionFactory,
				backupProvider
		);

		// Act

		myJmsRestaurant.open();
		Thread.sleep(duration);
		myJmsRestaurant.close();

		// Assert

		Map<MessagingStat, Long> stats = myJmsRestaurant.getStats();
		int expectedDishesToProduce = amount * pairs;
		assertThat(stats.get(MessagingStat.PRODUCER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.CONSUMER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.PRODUCER_OUT)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.CONSUMER_OUT)).isEqualTo(expectedDishesToProduce);
	}

	private ThreadPoolTaskScheduler workers(int pairs) {
		ThreadPoolTaskScheduler taskExecutor = new ThreadPoolTaskScheduler();
		taskExecutor.setThreadNamePrefix("worker-");
		taskExecutor.setPoolSize(pairs * 2);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.initialize();
		return taskExecutor;
	}
}
