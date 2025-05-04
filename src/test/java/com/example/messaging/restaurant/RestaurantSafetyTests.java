package com.example.messaging.restaurant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.restaurant.MyJmsRestaurant;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Slf4j
public class RestaurantSafetyTests {

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.INFO);
	}

	@Test
	void givenNDishes_thenServerAndCookNDishes() throws InterruptedException {

		int pairs = 3;
		int blockingQueueCapacity = 100;
		int trials = 10; // 10.000+ usually, 100.000 ideally
		int trialDurationMilis = 100;
		int dishesToProduce = 10000;

		testRestaurantSafety(trials, blockingQueueCapacity, pairs, trialDurationMilis, dishesToProduce);
	}

	void testRestaurantSafety(int trials, int capacity, int pairs, int duration, int amount) throws InterruptedException {

		ThreadPoolTaskExecutor workers = new ThreadPoolTaskExecutor();
		workers.setThreadNamePrefix("worker-");
		workers.setCorePoolSize(pairs * 2); // producers + consumers
		workers.setWaitForTasksToCompleteOnShutdown(true);
		workers.initialize();

		for (int i = 0; i < trials; i++) {
			restaurantTest(workers, capacity, pairs, duration, amount);
			log.info("Trial {} OK", i);
		}

		workers.destroy();
	}

	void restaurantTest(TaskExecutor workers, int capacity, int pairs, int duration, int amount) throws InterruptedException {
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
		Map<MessagingStat, Long> stats = myJmsRestaurant.getStats();
		myJmsRestaurant.close();

		// Assert

		int expectedDishesToProduce = amount * pairs;
		assertThat(stats.get(MessagingStat.PRODUCER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.CONSUMER_IN)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.PRODUCER_OUT)).isEqualTo(expectedDishesToProduce);
		assertThat(stats.get(MessagingStat.CONSUMER_OUT)).isEqualTo(expectedDishesToProduce);
	}
}
