package com.example.messaging.jms.backup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.model.Dish;
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

import static org.assertj.core.api.Assertions.assertThat;
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
	private BackupCheck backup;

	@Autowired
	private BackupProvider<Dish> backupProvider;

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.TRACE);
	}

	@Test
	void givenBrokerOn_whenBackedUpMessages_thenResend() throws Exception {

		// Arrange

		// start producer
		MockHttpServletResponse startProducer = mockMvc.perform(post("/jms/producer/start")).andReturn().getResponse();
		assertThat(startProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		// wait to send messages to back up
		Thread.sleep(1000);

		// stop producer
		MockHttpServletResponse stopProducer = mockMvc.perform(post("/jms/producer/stop")).andReturn().getResponse();
		assertThat(stopProducer.getStatus()).isEqualTo(HttpStatus.OK.value());

		assertThat(backupProvider.hasMoreElements()).isTrue();

		// Act

		artemis.start();

		// wait for broker to start
		Thread.sleep(10000);

		backup.backupCheck();

		// Assert

		assertThat(backupProvider.hasMoreElements()).isFalse();
		artemis.stop();
	}
}
