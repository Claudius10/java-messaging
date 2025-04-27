package com.example.messaging.jms.restaurant;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.producer.backup.BackupProducer;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsConnectionFactory;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
import com.example.messaging.jms.listener.MyExceptionListener;
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

		int trials = 10000;
		int trialDurationMilis = 100;
		int dishesToProduce = 10000;

		testRestaurantSafety(trials, blockingQueueCapacity, maxThreads, trialDurationMilis, dishesToProduce);
	}

	void testRestaurantSafety(int trials, int capacity, int pairs, int duration, int amount) throws InterruptedException {
		ThreadPoolTaskScheduler workers = workers();

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
		jmsProperties.setReconnectionMaxAttempts(5);

		JmsConnectionFactory jmsConnectionFactory = mock(JmsConnectionFactory.class);
		BackupProducer myBackupProducer = mock(BackupProducer.class);
		ExceptionListener myExceptionListener = mock(MyExceptionListener.class);
		CompletionListener myCompletionListener = mock(MyCompletionListener.class);

		MessagingManager myJmsRestaurant = new MyJmsRestaurant(
				workers,
				restaurantProperties,
				jmsProperties,
				jmsConnectionFactory,
				myExceptionListener,
				myCompletionListener,
				myBackupProducer);

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

	private ThreadPoolTaskScheduler workers() {
		ThreadPoolTaskScheduler taskExecutor = new ThreadPoolTaskScheduler();
		taskExecutor.setThreadNamePrefix("worker-");
		taskExecutor.setPoolSize(6);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.initialize();
		return taskExecutor;
	}
}
