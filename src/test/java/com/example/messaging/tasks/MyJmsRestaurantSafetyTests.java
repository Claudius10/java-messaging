package com.example.messaging.tasks;

import com.example.messaging.jms.config.JmsConnectionFactory;
import com.example.messaging.jms.restaurant.MyJmsRestaurant;
import com.example.messaging.common.restaurant.Restaurant;
import com.example.messaging.common.util.DishesStat;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.common.config.RestaurantProperties;
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

		int trials = 5000;
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
		restaurantProperties.setTakeGiveUp(2);
		restaurantProperties.setGreetTimeOut(99999);

		JmsProperties jmsProperties = new JmsProperties();
		jmsProperties.setUser("user");
		jmsProperties.setPassword("password");
		jmsProperties.setDestination("queue");
		jmsProperties.setProducer("NoopProducer");
		jmsProperties.setMaxConnections(pairs);

		JmsConnectionFactory jmsConnectionFactory = mock(JmsConnectionFactory.class);
		ExceptionListener exceptionListener = mock(ExceptionListener.class);
		CompletionListener completionListener = mock(CompletionListener.class);

		Restaurant restaurant = new MyJmsRestaurant(workers(), restaurantProperties, jmsProperties, jmsConnectionFactory, exceptionListener, completionListener);

		// Act

		restaurant.open();
		Thread.sleep(duration);
		restaurant.close();

		// Assert

		Map<DishesStat, Long> stats = restaurant.getStats();
		int expectedDishesToProduce = amount * pairs;
		assertThat(stats.get(DishesStat.PRODUCER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(DishesStat.CONSUMER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(DishesStat.PRODUCER_OUT)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(DishesStat.CONSUMER_OUT)).isEqualTo(expectedDishesToProduce);
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
