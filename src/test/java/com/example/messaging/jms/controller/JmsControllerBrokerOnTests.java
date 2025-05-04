package com.example.messaging.jms.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.util.MessagingStat;
import com.example.messaging.common.util.RestaurantProperties;
import com.example.messaging.jms.config.JmsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.backoff.FixedBackOff;
import org.testcontainers.activemq.ArtemisContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("Jms")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc
@Slf4j
public class JmsControllerBrokerOnTests {

	private final static ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:latest-alpine")
			.withUser("artemis")
			.withPassword("artemis");

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

		// 1000 trials -> OK (17 min)

		// Arrange

		// start producer
		MockHttpServletResponse startProducer = mockMvc.perform(post("/jms/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// Act

		// start consumer
		MockHttpServletResponse startConsumer = mockMvc.perform(post("/jms/consumer/start")).andReturn().getResponse();
		assertThat(startConsumer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// check if consumer is alive
		MockHttpServletResponse consumerRunning = mockMvc.perform(get("/jms/consumer/alive")).andReturn().getResponse();
		assertThat(consumerRunning.getContentAsString()).isEqualTo("true");

		// wait for work to complete
		Thread.sleep(1000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/jms/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// stop consumer
		MockHttpServletResponse stopConsumer = mockMvc.perform(post("/jms/consumer/stop")).andReturn().getResponse();
		assertThat(stopConsumer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// check if consumer is alive
		MockHttpServletResponse consumerRunningTwo = mockMvc.perform(get("/jms/consumer/alive")).andReturn().getResponse();
		assertThat(consumerRunningTwo.getContentAsString()).isEqualTo("false");

		// get consumer stats
		MockHttpServletResponse consumerStats = mockMvc.perform(get("/jms/consumer/stats/current")).andReturn().getResponse();
		assertThat(consumerStats.getStatus()).isEqualTo(HttpStatus.OK.value());

		// get producer stats
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/jms/producer/stats")).andReturn().getResponse();
		assertThat(producerStatsResponse.getStatus()).isEqualTo(HttpStatus.OK.value());

		// Assert

		final int expected = restaurantProperties.getDishesToProduce() * restaurantProperties.getMaxConnections();

		// assert producer stats
		Map stats = objectMapper.readValue(producerStatsResponse.getContentAsString(), Map.class);
		assertThat(stats.get(MessagingStat.PRODUCER_IN.toString())).isEqualTo(expected);
		assertThat(stats.get(MessagingStat.CONSUMER_IN.toString())).isEqualTo(expected);
		assertThat(stats.get(MessagingStat.PRODUCER_OUT.toString())).isEqualTo(expected);
		assertThat(stats.get(MessagingStat.CONSUMER_OUT.toString())).isEqualTo(expected);

		// assert consumer stats
		assertThat(Integer.valueOf(consumerStats.getContentAsString())).isEqualTo(expected);
	}

	@TestConfiguration
	public static class JmsConfig {

		@Bean
		@Primary
		ConnectionFactory connectionFactoryTest() {
			JmsProperties jmsProperties = jmsProperties();

			JmsPoolConnectionFactory connectionFactory = new JmsPoolConnectionFactory();
			connectionFactory.setConnectionFactory(new ActiveMQConnectionFactory(jmsProperties.getBrokerUrl(), jmsProperties.getUser(), jmsProperties.getPassword()));

			final int consumerAndProducersPair = jmsProperties.getMaxConnections() * 2;
			final int listeners = jmsProperties.getMaxConnections();
			final int scheduledTasksThatNeedAJmsConnection = 1;
			final int maxConnections = consumerAndProducersPair + listeners + scheduledTasksThatNeedAJmsConnection;

			connectionFactory.setMaxConnections(maxConnections);

			return connectionFactory;
		}

		@Bean
		@Primary
		DefaultJmsListenerContainerFactory jmsListenerContainerFactoryTest(ConnectionFactory connectionFactoryTest) {
			JmsProperties jmsProperties = jmsProperties();

			DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
			factory.setBackOff(new FixedBackOff(jmsProperties.getReconnectionIntervalMs(), 9999));
			factory.setConnectionFactory(connectionFactoryTest);
			factory.setConcurrency(String.valueOf(jmsProperties.getMaxConnections()));
			factory.setAutoStartup(false);
			return factory;
		}

		JmsProperties jmsProperties() {
			if (!artemis.isRunning()) {
				artemis.start();
				log.info("Artemis started");
			}

			JmsProperties jmsProperties = new JmsProperties();
			jmsProperties.setBrokerUrl(artemis.getBrokerUrl() + "?confirmationWindowSize=10240");
			jmsProperties.setUser(artemis.getUser());
			jmsProperties.setPassword(artemis.getPassword());
			jmsProperties.setDestination("queue-table-A");
			jmsProperties.setProducer("JmsProducer");
			jmsProperties.setMaxConnections(2);
			jmsProperties.setPollTimeOut(2);
			jmsProperties.setReconnectionIntervalMs(5000);

			log.info("Broker URL: {}", jmsProperties.getBrokerUrl());

			return jmsProperties;
		}
	}
}
