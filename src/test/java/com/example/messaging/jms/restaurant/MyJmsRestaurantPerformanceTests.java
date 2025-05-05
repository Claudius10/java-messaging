package com.example.messaging.jms.restaurant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.impl.MockBackupProvider;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.util.MessagingMetric;
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

	private final ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:latest-alpine")
			.withUser("artemis")
			.withPassword("artemis");

	private final List<Long> results = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.INFO);
	}

	@Test
	void testRestaurantPerformance() throws InterruptedException {

		int threadPairs = 3; // affects performance
		int queueCapacity = 100; // affects performance
		int trials = 1;
		int maxTestDurationMs = 10000; // ms
		int dishesToProduce = 10000; // affects performance
		int producerIdle = 0; // ms
		int consumerIdle = 1000; // ms

		testPerformance(trials, maxTestDurationMs, threadPairs, queueCapacity, dishesToProduce, producerIdle, consumerIdle);

		log.info("Average items sent under ten seconds over {} trials: {}", trials, results.stream().mapToDouble(Long::doubleValue).average().orElse(0.0));

		// RESULTS
		// 6 threads (3 producers - 3 consumers)
		// 1) 797.396 in 10 seconds (avg 10 trials)
	}

	void testPerformance(int trials, int maxTestDuration, int threadPairs, int queueCapacity, int dishesToProduce, int producerIdle, int consumerIdle) throws InterruptedException {

		ThreadPoolTaskExecutor workers = new ThreadPoolTaskExecutor();
		workers.setThreadNamePrefix("worker-");
		workers.setCorePoolSize(threadPairs * 2); // producers + consumers
		workers.setWaitForTasksToCompleteOnShutdown(true);
		workers.initialize();

		for (int i = 0; i < trials; i++) {
			artemis.start();
			restaurantTest(workers, maxTestDuration, threadPairs, queueCapacity, dishesToProduce, producerIdle, consumerIdle);
			artemis.stop();
		}

		workers.destroy();
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
		jmsProperties.setReconnectionIntervalMs(5000);

		log.info("Broker URL {}", jmsProperties.getBrokerUrl());

		ConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		JmsPoolConnectionFactory jmsConnectionFactory = (JmsPoolConnectionFactory) connectionFactory;
		jmsConnectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));
		jmsConnectionFactory.setMaxConnections(jmsProperties.getMaxConnections()); // only threads for producers, not using consumers for this test

		MessagingManager myJmsRestaurant = new MyJmsRestaurant(
				workers,
				restaurantProperties,
				jmsProperties,
				connectionFactory,
				new MockBackupProvider()
		);

		// Act

		myJmsRestaurant.open();
		Thread.sleep(duration);
		myJmsRestaurant.close();

		results.add(myJmsRestaurant.getStats().get(MessagingMetric.CONSUMER_OUT));
	}
}