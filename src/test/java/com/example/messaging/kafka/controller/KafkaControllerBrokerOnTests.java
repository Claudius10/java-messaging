package com.example.messaging.kafka.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.util.ConsumerMetric;
import com.example.messaging.common.util.ProducerMetric;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.kafka.config.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("Kafka")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc
@Slf4j
public class KafkaControllerBrokerOnTests {

	private final static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RestaurantProperties restaurantProperties;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.TRACE);
	}

	@RepeatedTest(value = 2, failureThreshold = 1)
	void givenConsumersOn_whenProducersOn_thenProduceAndConsume() throws Exception {

		// 500 trials -> OK (17 min)
		// NOTE - have to start consumers first

		// Arrange

		// start consumer
		MockHttpServletResponse startConsumer = mockMvc.perform(post("/kafka/consumer/start")).andReturn().getResponse();
		assertThat(startConsumer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// check if consumer is alive
		MockHttpServletResponse consumerRunning = mockMvc.perform(get("/kafka/consumer/alive")).andReturn().getResponse();
		assertThat(consumerRunning.getContentAsString()).isEqualTo("true");

		// wait for consumers to connect
		Thread.sleep(1000);

		// start producer
		MockHttpServletResponse startProducer = mockMvc.perform(post("/kafka/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// Act

		// wait for work to complete
		Thread.sleep(1000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/kafka/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// stop consumer
		MockHttpServletResponse stopConsumer = mockMvc.perform(post("/kafka/consumer/stop")).andReturn().getResponse();
		assertThat(stopConsumer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// check if consumer is alive
		MockHttpServletResponse consumerRunningTwo = mockMvc.perform(get("/kafka/consumer/alive")).andReturn().getResponse();
		assertThat(consumerRunningTwo.getContentAsString()).isEqualTo("false");

		// get consumer stats
		MockHttpServletResponse consumerStats = mockMvc.perform(get("/kafka/consumer/metrics")).andReturn().getResponse();
		assertThat(consumerStats.getStatus()).isEqualTo(HttpStatus.OK.value());

		// get producer stats
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponse.getStatus()).isEqualTo(HttpStatus.OK.value());

		// Assert

		final int expected = restaurantProperties.getDishesToProduce() * restaurantProperties.getMaxConnections();

		// assert producer stats
		Map producerMetrics = objectMapper.readValue(producerStatsResponse.getContentAsString(), Map.class);
		assertThat(producerMetrics.get(ProducerMetric.SENT.toString())).isEqualTo(expected);
		assertThat(producerMetrics.get(ProducerMetric.ERROR.toString())).isEqualTo(0);

		// assert consumer stats
		Map consumerMetrics = objectMapper.readValue(consumerStats.getContentAsString(), Map.class);
		assertThat(consumerMetrics.get(ConsumerMetric.CURRENT.toString())).isEqualTo(expected);
	}

	@TestConfiguration
	public static class KafkaConfig {

		@Bean
		@Primary
		KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Long, String>> kafkaListenerContainerFactoryTest(ConsumerFactory<Long, String> consumerFactoryTest) {
			KafkaProperties kafkaProperties = kafkaProperties();

			ConcurrentKafkaListenerContainerFactory<Long, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(consumerFactoryTest);
			factory.setConcurrency(kafkaProperties.getMaxConnections()); // only for the consumers
			factory.getContainerProperties().setPollTimeout(kafkaProperties.getPollTimeOut());
			factory.setAutoStartup(false);
			return factory;
		}

		@Bean
		@Primary
		ConsumerFactory<Long, String> consumerFactoryTest() {
			return new DefaultKafkaConsumerFactory<>(consumerConfigsTest());
		}

		Map<String, Object> consumerConfigsTest() {
			KafkaProperties kafkaProperties = kafkaProperties();

			Map<String, Object> props = new HashMap<>();
			props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
			props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProperties.getConsumerClientId());
			props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
			props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
			return props;
		}

		// Producer

		@Bean
		@Primary
		KafkaTemplate<Long, String> kafkaTemplateTest(ProducerFactory<Long, String> producerFactoryTest, ProducerListener<Long, String> myProducerListener) {
			KafkaTemplate<Long, String> template = new KafkaTemplate<>(producerFactoryTest);
			template.setProducerListener(myProducerListener);
			return template;
		}

		@Bean
		@Primary
		ProducerFactory<Long, String> producerFactoryTest() {
			DefaultKafkaProducerFactory<Long, String> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigsTest());
			producerFactory.setProducerPerThread(true); // does not affect performance, but it does affect safety!!!
			return producerFactory;
		}

		// https://kafka.apache.org/documentation/#producerconfigs
		Map<String, Object> producerConfigsTest() {
			KafkaProperties kafkaProperties = kafkaProperties();

			Map<String, Object> props = new HashMap<>();
			props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
			props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getProducerClientId());
			props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducerAckMode());
			props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, kafkaProperties.getProducerBlockMs()); // ms to wait before throwing when attempting to send
			props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getProducerTimeOutMs()); // ms between producer connection attempts
			props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
			return props;
		}

		@Bean
		@Primary
		NewTopic topicTest() {
			KafkaProperties kafkaProperties = kafkaProperties();

			return TopicBuilder.name(kafkaProperties.getTopic())
					.partitions(kafkaProperties.getMaxConnections())
					.replicas(1)
					.build();
		}

		KafkaProperties kafkaProperties() {
			if (!kafka.isRunning()) {
				kafka.start();
				log.info("Kafka started");
			}

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
			kafkaProperties.setMaxConnections(2);
			kafkaProperties.setPollTimeOut(2);

			log.info("Broker URL: {}", kafkaProperties.getBrokerUrl());
			System.setProperty("spring.kafka.bootstrap-servers", kafkaProperties.getBrokerUrl());

			return kafkaProperties;
		}
	}
}
