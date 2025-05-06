package com.example.messaging.jms.backup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.util.MessagingMetric;
import com.example.messaging.common.util.ProducerMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("Jms")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc
@Slf4j
public class JmsBackupTests {

	private final MyArtemisContainer artemis = new MyArtemisContainer();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.TRACE);
	}

	@RepeatedTest(value = 2, failureThreshold = 1)
	void givenMessages_whenBrokerOffBackupMessages_thenResendWhenBrokerOn() throws Exception {

		// Arrange

		// start producer when broker is off
		MockHttpServletResponse startProducer = mockMvc.perform(post("/jms/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait for work to be done
		Thread.sleep(2000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/jms/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// get producer metrics
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/jms/producer/metrics")).andReturn().getResponse();
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

		// should be all ERROR, none SENT
		int error = (int) stats.get(ProducerMetric.ERROR.toString());
		int sent = (int) stats.get(ProducerMetric.SENT.toString());
		int resent = (int) stats.get(ProducerMetric.RESENT.toString());

		assertThat(sent).isZero();
		assertThat(resent).isZero();
		assertThat(error).isEqualTo(consumerIn);
		assertThat(error).isEqualTo(consumerOut);
		assertThat(error).isEqualTo(producerIn);
		assertThat(error).isEqualTo(producerOut);

		// Act

		log.info("Starting broker");
		artemis.start();

		// wait for broker to start
		Thread.sleep(10000);

		// backup check
		MockHttpServletResponse backupCheck = mockMvc.perform(post("/jms/backup")).andReturn().getResponse();
		assertThat(backupCheck .getStatus()).isEqualTo(HttpStatus.OK.value());

		// Assert

		// get producer metrics
		MockHttpServletResponse producerStatsResponseTwo = mockMvc.perform(get("/jms/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponseTwo.getStatus()).isEqualTo(HttpStatus.OK.value());
		Map statsTwo = objectMapper.readValue(producerStatsResponseTwo.getContentAsString(), Map.class);

		// get restaurant metrics
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

		final int sentMinusTestRequest = sentTwo - 1; // jms checks whatever broker is online by sending a test request

		assertThat(resentTwo).isEqualTo(errorTwo);
		assertThat(resentTwo).isEqualTo(sentMinusTestRequest);

		assertThat(sentMinusTestRequest).isEqualTo(consumerInTwo);
		assertThat(sentMinusTestRequest).isEqualTo(consumerOutTwo);
		assertThat(sentMinusTestRequest).isEqualTo(producerInTwo);
		assertThat(sentMinusTestRequest).isEqualTo(producerOutTwo);

		log.info("Stopping broker");
		artemis.stop();
	}

	@RepeatedTest(value = 2, failureThreshold = 1)
	void givenBrokerOnSendMessages_whenBrokerOffBackupMessages_thenResendWhenBrokerOnAgain() throws Exception {

		// Arrange

		log.trace("Starting broker - start");
		artemis.start();

		// wait for broker to start
		Thread.sleep(10000);

		// start producer when broker is on
		MockHttpServletResponse startProducer = mockMvc.perform(post("/jms/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait to send messages to broker normally
		Thread.sleep(10000);

		log.trace("Stopping broker");
		artemis.stop();

		// wait to back up messages
		Thread.sleep(10000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/jms/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// get producer metrics
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/jms/producer/metrics")).andReturn().getResponse();
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
		artemis.start();

		// wait to for broker to start
		Thread.sleep(10000);

		// backup check
		MockHttpServletResponse backupCheck = mockMvc.perform(post("/jms/backup")).andReturn().getResponse();
		assertThat(backupCheck .getStatus()).isEqualTo(HttpStatus.OK.value());

		// Assert

		// get producer metrics
		MockHttpServletResponse producerStatsResponseTwo = mockMvc.perform(get("/jms/producer/metrics")).andReturn().getResponse();
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

		final int sentTwoMinusTestRequest = sentTwo - 1;

		assertThat(resentTwo).isGreaterThan(0);
		assertThat(errorTwo).isGreaterThan(0);
		assertThat(sentTwoMinusTestRequest).isGreaterThan(0);

		assertThat(resentTwo).isEqualTo(errorTwo);

		assertThat(sentTwoMinusTestRequest).isGreaterThan(resentTwo);

		assertThat(sentTwoMinusTestRequest).isEqualTo(consumerInTwo);
		assertThat(sentTwoMinusTestRequest).isEqualTo(consumerOutTwo);
		assertThat(sentTwoMinusTestRequest).isEqualTo(producerInTwo);
		assertThat(sentTwoMinusTestRequest).isEqualTo(producerOutTwo);

		log.trace("Stopping broker - end");
		artemis.stop();
	}
}
