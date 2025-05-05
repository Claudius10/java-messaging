package com.example.messaging.kafka.backup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.util.ProducerMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	private BackupCheck backup;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.TRACE);
	}

	@Test
	void givenBrokerOn_whenBackedUpMessages_thenResend() throws Exception {

		// Arrange

		// start producer
		MockHttpServletResponse startProducer = mockMvc.perform(post("/kafka/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait to back up messages
		Thread.sleep(1000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/kafka/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// get producer stats
		MockHttpServletResponse producerStatsResponse = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponse.getStatus()).isEqualTo(HttpStatus.OK.value());

		Map stats = objectMapper.readValue(producerStatsResponse.getContentAsString(), Map.class);

		assertThat(stats.get(ProducerMetric.ERROR.toString())).isNotEqualTo(0);
		assertThat(stats.get(ProducerMetric.RESENT.toString())).isEqualTo(0);

		// Act

		kafka.start();

		// wait for broker to start
		Thread.sleep(10000);

		backup.backupCheck();

		// Assert

		MockHttpServletResponse producerStatsResponseTwo = mockMvc.perform(get("/kafka/producer/metrics")).andReturn().getResponse();
		assertThat(producerStatsResponseTwo.getStatus()).isEqualTo(HttpStatus.OK.value());

		Map statsTwo = objectMapper.readValue(producerStatsResponseTwo.getContentAsString(), Map.class);
		int error = (int) statsTwo.get(ProducerMetric.ERROR.toString());
		int resent = (int) statsTwo.get(ProducerMetric.RESENT.toString());
		int sent = (int) statsTwo.get(ProducerMetric.SENT.toString());
		int total = (int) statsTwo.get(ProducerMetric.TOTAL.toString());

		assertThat(resent).isEqualTo(error);
		assertThat(total).isEqualTo(error + sent);

		kafka.stop();
	}
}
