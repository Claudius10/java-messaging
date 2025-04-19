package com.example.messaging.tasks;

import com.example.messaging.jms.JmsConnectionFactory;
import com.example.messaging.restaurant.MyJmsRestaurant;
import com.example.messaging.util.DishesStat;
import com.example.messaging.util.JmsProperties;
import com.example.messaging.util.RestaurantProperties;
import jakarta.jms.CompletionListener;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MyJmsRestaurantSafetyTests {

	@Test
	void givenNDishes_thenServerAndCookNDishes() throws InterruptedException, JMSException {

		int dishesToProduce = 10000;
		int blockingQueueCapacity = 1000;
		int maxThreads = 3;
		int trials = 100;
		int trialDurationMilis = 2000;

		testRestaurantSafety(trials, blockingQueueCapacity, maxThreads, trialDurationMilis, dishesToProduce);
	}

	void testRestaurantSafety(int trials, int capacity, int pairs, int duration, int amount) throws InterruptedException, JMSException {
		for (int i = 0; i < trials; i++) {
			restaurantTest(capacity, pairs, duration, amount);
		}
	}

	void restaurantTest(int capacity, int pairs, int duration, int amount) throws InterruptedException, JMSException {
		// Arrange

		RestaurantProperties restaurantProperties = new RestaurantProperties();
		restaurantProperties.setDishes(amount);
		restaurantProperties.setDishesCapacity(capacity);
		restaurantProperties.setTakeGiveUp(2);
		restaurantProperties.setMaxCapacity(pairs);
		restaurantProperties.setGreetTimeOut(99999);

		JmsProperties jmsProperties = new JmsProperties();
		jmsProperties.setUser("user");
		jmsProperties.setPassword("password");
		jmsProperties.setDestination("queue");
		jmsProperties.setProducer("NoopProducer");

		JmsConnectionFactory jmsConnectionFactory = mock(JmsConnectionFactory.class);
		ExceptionListener exceptionListener = mock(ExceptionListener.class);
		CompletionListener completionListener = mock(CompletionListener.class);

		MyJmsRestaurant restaurant = new MyJmsRestaurant(workers(), restaurantProperties, jmsProperties, jmsConnectionFactory, exceptionListener, completionListener);

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
