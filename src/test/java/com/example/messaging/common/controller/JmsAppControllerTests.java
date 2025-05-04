package com.example.messaging.common.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.example.messaging.common.util.APIResponses;
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
@AutoConfigureMockMvc
@Slf4j
public class JmsAppControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger("com.example.messaging");
		logger.setLevel(Level.TRACE);
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	void givenStop_whenWorking_thenReturnBadRequest() throws Exception {

		// Arrange

		MockHttpServletResponse start = mockMvc.perform(post("/jms/producer/start")).andReturn().getResponse();
		assertThat(start.getStatus()).isEqualTo(HttpStatus.OK.value());

		// Act

		MockHttpServletResponse response = mockMvc.perform(post("/stop")).andReturn().getResponse();

		// Assert

		assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.getContentAsString()).isEqualTo(APIResponses.SHUTDOWN_WORKING_WARN);
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	void givenStop_whenAllowedToStop_thenReturnOk() throws Exception {

		// Arrange

		MockHttpServletResponse start = mockMvc.perform(post("/jms/producer/start")).andReturn().getResponse();
		assertThat(start.getStatus()).isEqualTo(HttpStatus.OK.value());

		// Act

		MockHttpServletResponse stop = mockMvc.perform(post("/jms/producer/stop")).andReturn().getResponse();
		assertThat(stop.getStatus()).isEqualTo(HttpStatus.OK.value());

		MockHttpServletResponse response = mockMvc.perform(post("/stop")).andReturn().getResponse();

		// Assert

		assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
	}
}
