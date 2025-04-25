package com.example.messaging.tasks;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.producer.backup.BackupProducer;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsConnectionFactory;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.restaurant.MyJmsRestaurant;
import jakarta.jms.CompletionListener;
import jakarta.jms.ExceptionListener;
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

		int maxThreads = 3;
		int blockingQueueCapacity = 1000;

		int trials = 1; // TODO - find why it bugs out after 5000+
		int trialDurationMilis = 100;
		int dishesToProduce = 10000;

		testRestaurantSafety(trials, blockingQueueCapacity, maxThreads, trialDurationMilis, dishesToProduce);
	}

	void testRestaurantSafety(int trials, int capacity, int pairs, int duration, int amount) throws InterruptedException {
		for (int i = 0; i < trials; i++) {
			restaurantTest(capacity, pairs, duration, amount);
			log.info("Trial {} OK", i);
		}
	}

	void restaurantTest(int capacity, int pairs, int duration, int amount) throws InterruptedException {
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

		JmsConnectionFactory jmsConnectionFactory = mock(JmsConnectionFactory.class);
		ExceptionListener exceptionListener = mock(ExceptionListener.class);
		CompletionListener completionListener = mock(CompletionListener.class);
		BackupProducer myBackupProducer = mock(BackupProducer.class);

		MessagingManager messagingManager = new MyJmsRestaurant(workers(), restaurantProperties, jmsProperties, jmsConnectionFactory, exceptionListener, completionListener, myBackupProducer);

		// Act

		messagingManager.open();
		Thread.sleep(duration);
		messagingManager.close();

		// Assert

		Map<MessagingStat, Long> stats = messagingManager.getStats();
		int expectedDishesToProduce = amount * pairs;
		assertThat(stats.get(MessagingStat.PRODUCER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.CONSUMER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.PRODUCER_OUT)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.CONSUMER_OUT)).isEqualTo(expectedDishesToProduce);
	}

	ThreadPoolTaskScheduler workers() {
		ThreadPoolTaskScheduler taskExecutor = new ThreadPoolTaskScheduler();
		taskExecutor.setThreadNamePrefix("worker-");
		taskExecutor.setPoolSize(25);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.initialize();
		return taskExecutor;
	}
}
