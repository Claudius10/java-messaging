package com.example.messaging.tasks;

import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsConnectionFactory;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
import com.example.messaging.jms.listener.MyExceptionListener;
import com.example.messaging.jms.producer.impl.MyJmsBackupProducer;
import com.example.messaging.jms.restaurant.MyJmsRestaurant;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StopWatch;
import org.testcontainers.activemq.ArtemisContainer;

@Slf4j
public class MyJmsRestaurantPerformanceTests {

	static ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:latest-alpine")
			.withUser("artemis")
			.withPassword("artemis");

	@Test
	void testJmsRestaurantPerformance() throws InterruptedException {
		int trials = 10;
		int maxTestDurationMs = 10000;
		int threadPairs = 3; // affects performance
		int queueCapacity = 100; // affects performance
		int dishesToProduce = 1000000;
		testPerformance(trials, maxTestDurationMs, threadPairs, queueCapacity, dishesToProduce);
	}

	void testPerformance(int trials, int maxTestDuration, int threadPairs, int queueCapacity, int dishesToProduce) throws InterruptedException {
		// dishes to produce = 1.000.000
		// how many dishes can be served in 10 seconds?
		for (int i = 0; i < trials; i++) {
			artemis.start();
			restaurantTest(maxTestDuration, threadPairs, queueCapacity, dishesToProduce);
			artemis.stop();
		}
	}

	void restaurantTest(int duration, int pairs, int queueCapacity, int dishesToProduce) throws InterruptedException {

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
		jmsProperties.setFactory("ActiveMQConnectionFactory");
		jmsProperties.setProducer("JmsProducer");
		jmsProperties.setMaxConnections(pairs);
		jmsProperties.setPollTimeOut(2);

		log.info("Broker URL {}", jmsProperties.getBrokerUrl());

		JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
		connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));
		connectionFactory.setMaxConnections(jmsProperties.getMaxConnections());

		JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connectionFactory);

		MessagingManager myJmsRestaurant = new MyJmsRestaurant(
				workers(),
				restaurantProperties,
				jmsProperties,
				jmsConnectionFactory,
				new MyExceptionListener(),
				new MyCompletionListener(),
				new MyJmsBackupProducer());

		StopWatch stopWatch = new StopWatch("JMS Restaurant");
		String details = String.format("ThreadPairs %s - queueCapacity %s", pairs, queueCapacity);

		// Act

		stopWatch.start(details);
		myJmsRestaurant.open();
		Thread.sleep(duration);
		myJmsRestaurant.close();
		stopWatch.stop();

		// Assert

		log.info(stopWatch.prettyPrint());
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
