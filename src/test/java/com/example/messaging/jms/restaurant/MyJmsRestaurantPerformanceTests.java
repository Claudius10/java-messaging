package com.example.messaging.jms.restaurant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsConnectionFactory;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
import com.example.messaging.jms.listener.MyExceptionListener;
import com.example.messaging.jms.producer.impl.MyJmsBackupProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
		int trials = 10;
		int maxTestDurationMs = 10000;
		int threadPairs = 3; // affects performance
		int queueCapacity = 100; // affects performance
		int dishesToProduce = 1000000;
		testPerformance(trials, maxTestDurationMs, threadPairs, queueCapacity, dishesToProduce);

		log.info("Average items sent under ten seconds over {} trials: {}", trials, results.stream().mapToDouble(Long::doubleValue).average().orElse(0.0));
	}

	void testPerformance(int trials, int maxTestDuration, int threadPairs, int queueCapacity, int dishesToProduce) throws InterruptedException {
		// dishes to produce = 1.000.000
		// how many dishes can be served in 10 seconds?

		ThreadPoolTaskScheduler workers = workers();

		for (int i = 0; i < trials; i++) {
			artemis.start();
			restaurantTest(workers, maxTestDuration, threadPairs, queueCapacity, dishesToProduce);
			artemis.stop();
		}
	}

	void restaurantTest(ThreadPoolTaskScheduler workers, int duration, int pairs, int queueCapacity, int dishesToProduce) throws InterruptedException {

		// Arrange

		RestaurantProperties restaurantProperties = new RestaurantProperties();
		restaurantProperties.setDishesToProduce(dishesToProduce);
		restaurantProperties.setDishesQueueCapacity(queueCapacity);
		restaurantProperties.setConsumerIdle(999999);
		restaurantProperties.setProducerIdle(999999);

		JmsProperties jmsProperties = new JmsProperties();
		jmsProperties.setBrokerUrl(artemis.getBrokerUrl() + "?confirmationWindowSize=10240");
		jmsProperties.setUser(artemis.getUser());
		jmsProperties.setPassword(artemis.getPassword());
		jmsProperties.setDestination("queue-table-A");
		jmsProperties.setProducer("JmsProducer");
		jmsProperties.setMaxConnections(pairs * 2);
		jmsProperties.setPollTimeOut(2);
		jmsProperties.setConsumerClientId("consumer");
		jmsProperties.setReconnectionIntervalMs(5000);
		jmsProperties.setReconnectionMaxAttempts(5);

		log.info("Broker URL {}", jmsProperties.getBrokerUrl());

		JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));
		connectionFactory.setMaxConnections(jmsProperties.getMaxConnections());

		JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connectionFactory);

		MessagingManager myJmsRestaurant = new MyJmsRestaurant(
				workers,
				restaurantProperties,
				jmsProperties,
				jmsConnectionFactory,
				new MyExceptionListener(),
				new MyCompletionListener(),
				new MyJmsBackupProducer());

		// Act

		myJmsRestaurant.open();
		Thread.sleep(duration);
		myJmsRestaurant.close();

		results.add(myJmsRestaurant.getStats().get(MessagingStat.CONSUMER_OUT));
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

/*
 --- RESULTS ---

 a) with JMXContext and JMSProducer per thread -> 295955 (avg 10 trials)
 b) with JMSProducer per thread (same JMSContext for all threads) -> 271152 (avg 10 trials)
 c) same JMSContext and JMSProducer for all threads -> 274172 (avg 10 trials)

 */