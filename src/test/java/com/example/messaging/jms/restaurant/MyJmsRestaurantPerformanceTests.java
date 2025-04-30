package com.example.messaging.jms.restaurant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.backup.impl.MockBackupProvider;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsProperties;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.activemq.ArtemisContainer;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MyJmsRestaurantPerformanceTests {

	private final static ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:latest-alpine")
			.withUser("artemis")
			.withPassword("artemis");

	private final List<Long> results = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		final Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.INFO);
	}

	@Test
	void testRestaurantPerformance() throws InterruptedException {

		int threadPairs = 3; // affects performance
		int queueCapacity = 100; // affects performance
		int trials = 10;
		int maxTestDurationMs = 10000; // ms
		int dishesToProduce = 10000; // affects performance
		int producerIdle = 0; // ms
		int consumerIdle = 1000; // ms

		testPerformance(trials, maxTestDurationMs, threadPairs, queueCapacity, dishesToProduce, producerIdle, consumerIdle);

		log.info("Average items sent under ten seconds over {} trials: {}", trials, results.stream().mapToDouble(Long::doubleValue).average().orElse(0.0));
	}

	void testPerformance(int trials, int maxTestDuration, int threadPairs, int queueCapacity, int dishesToProduce, int producerIdle, int consumerIdle) throws InterruptedException {
		// dishes to produce = 1.000.000
		// how many dishes can be served in 10 seconds?

		TaskExecutor workers = workers(threadPairs);

		for (int i = 0; i < trials; i++) {
			artemis.start();
			restaurantTest(workers, maxTestDuration, threadPairs, queueCapacity, dishesToProduce, producerIdle, consumerIdle);
			artemis.stop();
		}
	}

	void restaurantTest(TaskExecutor workers, int duration, int pairs, int queueCapacity, int dishesToProduce, int producerIdle, int consumerIdle) throws InterruptedException {

		// Arrange

		RestaurantProperties restaurantProperties = new RestaurantProperties();
		restaurantProperties.setDishesToProduce(dishesToProduce);
		restaurantProperties.setDishesQueueCapacity(queueCapacity);
		restaurantProperties.setProducerIdle(producerIdle);
		restaurantProperties.setConsumerIdle(consumerIdle);

		JmsProperties jmsProperties = new JmsProperties();
		jmsProperties.setBrokerUrl(artemis.getBrokerUrl() + "?confirmationWindowSize=10240");
		jmsProperties.setUser(artemis.getUser());
		jmsProperties.setPassword(artemis.getPassword());
		jmsProperties.setDestination("queue-table-A");
		jmsProperties.setProducer("JmsProducer");
		jmsProperties.setMaxConnections(pairs);
		jmsProperties.setPollTimeOut(2);
		jmsProperties.setConsumerClientId("consumer");
		jmsProperties.setReconnectionIntervalMs(5000);

		log.info("Broker URL {}", jmsProperties.getBrokerUrl());

		ConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		JmsPoolConnectionFactory jmsConnectionFactory = (JmsPoolConnectionFactory) connectionFactory;
		jmsConnectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));
		jmsConnectionFactory.setMaxConnections(jmsProperties.getMaxConnections()); // only threads for producers, not using consumers for this test

		BackupProvider<Dish> backupProvider = new MockBackupProvider();

		MessagingManager myJmsRestaurant = new MyJmsRestaurant(
				workers,
				restaurantProperties,
				jmsProperties,
				connectionFactory,
				backupProvider
		);

		// Act

		myJmsRestaurant.open();
		Thread.sleep(duration);
		myJmsRestaurant.close();

		results.add(myJmsRestaurant.getStats().get(MessagingStat.CONSUMER_OUT));
	}

	TaskExecutor workers(int pairs) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("worker-");
		taskExecutor.setCorePoolSize(pairs * 2); // producers + consumers
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.initialize();
		return taskExecutor;
	}
}

/*
 --- RESULTS ---

 6 threads (3 producers - 3 consumers)

 1) 797.396 in 10 seconds (avg 10 trials)

 */