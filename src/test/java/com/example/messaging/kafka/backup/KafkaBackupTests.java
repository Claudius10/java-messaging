package com.example.messaging.kafka.backup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.util.MessagingMetric;
import com.example.messaging.common.util.ProducerMetric;
import com.example.messaging.kafka.config.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("Kafka")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc
@Slf4j
public class KafkaBackupTests {

	private final MyKafkaContainer kafka = new MyKafkaContainer();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private KafkaProperties kafkaProperties;

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.TRACE);
	}

	@RepeatedTest(value = 2, failureThreshold = 1)
	void givenMessages_whenBrokerOffBackupMessages_thenResendWhenBrokerOn() throws Exception {

		// Arrange

		// start producer when broker is off
		MockHttpServletResponse startProducer = mockMvc.perform(post("/kafka/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait for work to be done
		Thread.sleep(2000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/kafka/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// get producer metrics
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map stats = objectMapper.readValue(producerStatsResponse.getContentAsString(), Map.class);

		// get restaurant metrics
		MockHttpServletResponse restaurantMetrics = mockMvc.perform(get("/metrics")).andReturn().getResponse();
		assertThat(restaurantMetrics.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map restaurant = objectMapper.readValue(restaurantMetrics.getContentAsString(), Map.class);

		int consumerIn = (int) restaurant.get(MessagingMetric.CONSUMER_IN.toString());
		int consumerOut = (int) restaurant.get(MessagingMetric.CONSUMER_OUT.toString());
		int producerIn = (int) restaurant.get(MessagingMetric.PRODUCER_IN.toString());
		int producerOut = (int) restaurant.get(MessagingMetric.PRODUCER_OUT.toString());

		int error = (int) stats.get(ProducerMetric.ERROR.toString());
		int sent = (int) stats.get(ProducerMetric.SENT.toString());
		int resent = (int) stats.get(ProducerMetric.RESENT.toString());

		// should be all ERROR, none SENT

		assertThat(sent).isZero();
		assertThat(resent).isZero();

		assertThat(error).isEqualTo(consumerIn);
		assertThat(error).isEqualTo(consumerOut);
		assertThat(error).isEqualTo(producerIn);
		assertThat(error).isEqualTo(producerOut);

		// Act

		log.trace("Starting broker");
		kafka.start();
		createTopic(kafkaProperties.getBrokerUrl(), kafkaProperties.getTopic()); // initial creation by NewTopic bean fails due to broker not being started when context loads
		// wait for broker to start
		Thread.sleep(10000);

		// backup check
		MockHttpServletResponse backupCheck = mockMvc.perform(post("/kafka/backup")).andReturn().getResponse();
		assertThat(backupCheck .getStatus()).isEqualTo(HttpStatus.OK.value());

		// Assert

		MockHttpServletResponse producerStatsResponseTwo = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponseTwo.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map statsTwo = objectMapper.readValue(producerStatsResponseTwo.getContentAsString(), Map.class);

		MockHttpServletResponse restaurantMetricsTwo = mockMvc.perform(get("/metrics")).andReturn().getResponse();
		assertThat(restaurantMetricsTwo.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map restaurantTwo = objectMapper.readValue(restaurantMetricsTwo.getContentAsString(), Map.class);

		int consumerInTwo = (int) restaurantTwo.get(MessagingMetric.CONSUMER_IN.toString());
		int consumerOutTwo = (int) restaurantTwo.get(MessagingMetric.CONSUMER_OUT.toString());
		int producerInTwo = (int) restaurantTwo.get(MessagingMetric.PRODUCER_IN.toString());
		int producerOutTwo = (int) restaurantTwo.get(MessagingMetric.PRODUCER_OUT.toString());

		int errorTwo = (int) statsTwo.get(ProducerMetric.ERROR.toString());
		int resentTwo = (int) statsTwo.get(ProducerMetric.RESENT.toString());
		int sentTwo = (int) statsTwo.get(ProducerMetric.SENT.toString());

		assertThat(resentTwo).isGreaterThan(0);
		assertThat(errorTwo).isGreaterThan(0);
		assertThat(sentTwo).isGreaterThan(0);

		assertThat(resentTwo).isEqualTo(errorTwo);
		assertThat(resentTwo).isEqualTo(sentTwo);

		assertThat(sentTwo).isEqualTo(consumerInTwo);
		assertThat(sentTwo).isEqualTo(consumerOutTwo);
		assertThat(sentTwo).isEqualTo(producerInTwo);
		assertThat(sentTwo).isEqualTo(producerOutTwo);

		log.trace("Stopping broker");
		kafka.stop();
	}

	@RepeatedTest(value = 2, failureThreshold = 1)
	void givenBrokerOnSendMessages_whenBrokerOffBackupMessages_thenResendWhenBrokerOnAgain() throws Exception {

		// Arrange

		log.trace("Starting broker - start");
		kafka.start();
		createTopic(kafkaProperties.getBrokerUrl(), kafkaProperties.getTopic()); // initial creation by NewTopic bean fails due to broker not being started when context loads

		// wait for broker to start
		Thread.sleep(10000);

		// start producer when broker is on
		MockHttpServletResponse startProducer = mockMvc.perform(post("/kafka/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait to send messages to broker normally
		Thread.sleep(10000);

		log.trace("Stopping broker");
		kafka.stop();

		// wait to back up messages
		Thread.sleep(10000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/kafka/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait for acks
		Thread.sleep(2500); // ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG + 500

		// get producer metrics
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map producer = objectMapper.readValue(producerStatsResponse.getContentAsString(), Map.class);

		// get restaurant metrics
		MockHttpServletResponse restaurantMetrics = mockMvc.perform(get("/metrics")).andReturn().getResponse();
		assertThat(restaurantMetrics.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map restaurant = objectMapper.readValue(restaurantMetrics.getContentAsString(), Map.class);

		int consumerIn = (int) restaurant.get(MessagingMetric.CONSUMER_IN.toString());
		int consumerOut = (int) restaurant.get(MessagingMetric.CONSUMER_OUT.toString());
		int producerIn = (int) restaurant.get(MessagingMetric.PRODUCER_IN.toString());
		int producerOut = (int) restaurant.get(MessagingMetric.PRODUCER_OUT.toString());

		int error = (int) producer.get(ProducerMetric.ERROR.toString());
		int resent = (int) producer.get(ProducerMetric.RESENT.toString());
		int sent = (int) producer.get(ProducerMetric.SENT.toString());

		assertThat(resent).isZero();
		assertThat(error).isGreaterThan(0);
		final int total = sent + error;
		assertThat(total).isGreaterThan(0);
		assertThat(total).isEqualTo(consumerIn);
		assertThat(total).isEqualTo(consumerOut);
		assertThat(total).isEqualTo(producerIn);
		assertThat(total).isEqualTo(producerOut);

		// Act

		log.trace("Starting broker");
		kafka.start();
		createTopic(kafkaProperties.getBrokerUrl(), kafkaProperties.getTopic()); // initial creation by NewTopic bean fails due to broker not being started when context loads

		// wait to for broker to start
		Thread.sleep(10000);

		// backup check
		MockHttpServletResponse backupCheck = mockMvc.perform(post("/kafka/backup")).andReturn().getResponse();
		assertThat(backupCheck .getStatus()).isEqualTo(HttpStatus.OK.value());

		// Assert

		// get producer metrics
		MockHttpServletResponse producerStatsResponseTwo = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponseTwo.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map producerTwo = objectMapper.readValue(producerStatsResponseTwo.getContentAsString(), Map.class);

		// get restaurant metrics
		MockHttpServletResponse restaurantMetricsTwo = mockMvc.perform(get("/metrics")).andReturn().getResponse();
		assertThat(restaurantMetricsTwo.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map restaurantTwo = objectMapper.readValue(restaurantMetricsTwo.getContentAsString(), Map.class);

		int consumerInTwo = (int) restaurantTwo.get(MessagingMetric.CONSUMER_IN.toString());
		int consumerOutTwo = (int) restaurantTwo.get(MessagingMetric.CONSUMER_OUT.toString());
		int producerInTwo = (int) restaurantTwo.get(MessagingMetric.PRODUCER_IN.toString());
		int producerOutTwo = (int) restaurantTwo.get(MessagingMetric.PRODUCER_OUT.toString());

		int errorTwo = (int) producerTwo.get(ProducerMetric.ERROR.toString());
		int resentTwo = (int) producerTwo.get(ProducerMetric.RESENT.toString());
		int sentTwo = (int) producerTwo.get(ProducerMetric.SENT.toString());

		assertThat(resentTwo).isGreaterThan(0);
		assertThat(errorTwo).isGreaterThan(0);
		assertThat(sentTwo).isGreaterThan(0);
		assertThat(resentTwo).isEqualTo(errorTwo);
		assertThat(sentTwo).isGreaterThan(resentTwo);
		assertThat(sentTwo).isEqualTo(consumerInTwo);
		assertThat(sentTwo).isEqualTo(consumerOutTwo);
		assertThat(sentTwo).isEqualTo(producerInTwo);
		assertThat(sentTwo).isEqualTo(producerOutTwo);

		log.trace("Stopping broker - end");
		kafka.stop();
	}

	private void createTopic(String url, String topicName) {
		log.trace("Creating topic {}", topicName);
		Properties config = new Properties();
		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, url);

		Map<String, KafkaFuture<Void>> topicStatus;

		try (AdminClient localKafkaAdmin = AdminClient.create(config)) {

			NewTopic topic = new NewTopic(topicName, 2, (short) 1);
			List<NewTopic> topics = List.of(topic);

			topicStatus = localKafkaAdmin.createTopics(topics).values();
		}

		log.info(topicStatus.keySet().toString());
	}
}
