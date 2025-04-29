package com.example.messaging.kafka.restaurant;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.backup.impl.NoopBackupProvider;
import com.example.messaging.common.manager.MessagingManager;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.kafka.admin.MyKafkaAdmin;
import com.example.messaging.kafka.config.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

@Slf4j
public class MyKafkaRestaurantPerformanceTests {

	private final static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));

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
		int maxTestDurationMs = 10000;
		int dishesToProduce = 1000000;

		testPerformance(trials, maxTestDurationMs, threadPairs, queueCapacity, dishesToProduce);

		log.info("Average items sent under ten seconds over {} trials: {}", trials, results.stream().mapToDouble(Long::doubleValue).average().orElse(0.0));
	}

	void testPerformance(int trials, int maxTestDuration, int threadPairs, int queueCapacity, int dishesToProduce) throws InterruptedException {
		// dishes to produce = 1.000.000
		// how many dishes can be served in 10 seconds?

		ThreadPoolTaskScheduler workers = workers(threadPairs);

		for (int i = 0; i < trials; i++) {
			kafka.start();
			createTopic(kafka.getBootstrapServers(), "table-A");
			restaurantTest(workers, maxTestDuration, threadPairs, queueCapacity, dishesToProduce);
			kafka.stop();
		}
	}

	void restaurantTest(ThreadPoolTaskScheduler workers, int duration, int pairs, int queueCapacity, int dishesToProduce) throws InterruptedException {

		// Arrange

		RestaurantProperties restaurantProperties = new RestaurantProperties();
		restaurantProperties.setDishesToProduce(dishesToProduce);
		restaurantProperties.setDishesQueueCapacity(queueCapacity);
		restaurantProperties.setConsumerIdle(999999999);
		restaurantProperties.setProducerIdle(999999999);

		KafkaProperties kafkaProperties = new KafkaProperties();
		kafkaProperties.setBrokerUrl(kafka.getBootstrapServers());
		kafkaProperties.setClientId("MyKafkaRestaurant");
		kafkaProperties.setProducerClientId("producer");
		kafkaProperties.setProducerTimeOutMs(30000);
		kafkaProperties.setProducerBlockMs(1000);
		kafkaProperties.setProducerAckMode("1");
		kafkaProperties.setProducer("Kafka");
		kafkaProperties.setConsumerClientId("consumer");
		kafkaProperties.setConsumerGroupId("dishes");
		kafkaProperties.setConsumerTimeoutMs(30000);
		kafkaProperties.setTopic("table-A");
		kafkaProperties.setMaxConnections(pairs);
		kafkaProperties.setPollTimeOut(2);

		log.info("Broker URL {}", kafkaProperties.getBrokerUrl());

		ProducerFactory<Long, String> producerFactory = producerFactory(kafkaProperties);

		KafkaTemplate<Long, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
		KafkaAdmin kafkaAdmin = kafkaTemplate.getKafkaAdmin();
		MyKafkaAdmin myKafkaAdmin = new MyKafkaAdmin(kafkaAdmin);
		BackupProvider<Dish> backupProvider = new NoopBackupProvider();

		MessagingManager myJmsRestaurant = new MyKafkaRestaurant(
				workers,
				restaurantProperties,
				kafkaProperties,
				kafkaTemplate,
				myKafkaAdmin,
				backupProvider
		);

		// Act

		myJmsRestaurant.open();
		Thread.sleep(duration);
		myJmsRestaurant.close();

		results.add(myJmsRestaurant.getStats().get(MessagingStat.CONSUMER_OUT));
	}

	private void createTopic(String url, String topicName) {
		Properties config = new Properties();
		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, url);

		Map<String, KafkaFuture<Void>> topicStatus;

		try (AdminClient localKafkaAdmin = AdminClient.create(config)) {

			NewTopic topic = new NewTopic(topicName, 3, (short) 1);
			List<NewTopic> topics = List.of(topic);

			topicStatus = localKafkaAdmin.createTopics(topics).values();
		}

		log.info(topicStatus.keySet().toString());
	}

	private ProducerFactory<Long, String> producerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
		props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getProducerClientId());
		props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducerAckMode());
		props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, kafkaProperties.getProducerBlockMs()); // ms to wait before throwing when attempting to send
		props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getProducerTimeOutMs()); // ms between producer connection attempts
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		DefaultKafkaProducerFactory<Long, String> producerFactory = new DefaultKafkaProducerFactory<>(props);
		producerFactory.setProducerPerThread(false);
		return producerFactory;
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

/*
 --- RESULTS ---

 a) each thread with its own KafkaTemplate: 363319 (avg 10 trials)
 b) all threads using the same KafkaTemplate: 361412 (avg 10 trials) / 381113 (avg 10 trials)

 */