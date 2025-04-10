package com.example.messaging.controllers;

import com.example.messaging.activemq.ActiveMQPublisher;
import com.example.messaging.contracts.Publisher;
import com.example.messaging.model.JmsTextMessageDTO;
import com.example.messaging.tasks.MessagePublisherTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Controller
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class Controller {

	private final JmsTemplate jmsTemplate;

	private final TaskExecutor publisherTaskExecutor;

	@PostMapping("/publish/{amount}")
	public void sendMessages(@PathVariable int amount) {

		List<JmsTextMessageDTO> messages = new ArrayList<>();
		for (int i = 0; i < amount; i++) {
			messages.add(new JmsTextMessageDTO(i, "Hello World"));
		}

		StopWatch stopWatch = new StopWatch();

		String multiThreadedTaskName = "MultiThreaded Publisher";
		String singleThreadedTaskName = "SingleThreaded Publisher";

		Publisher publisher = new ActiveMQPublisher(jmsTemplate, new ActiveMQQueue("testQueue"));

		stopWatch.start(multiThreadedTaskName);
		publisherTaskExecutor.execute(new MessagePublisherTask(publisher, messages));
		stopWatch.stop();
		logPerf(multiThreadedTaskName, stopWatch.getTotalTimeMillis(), stopWatch.getTotalTimeSeconds());

		stopWatch.start(singleThreadedTaskName);
		for (int i = 1; i <= amount; i++) {
			publisher.publishMessage(new JmsTextMessageDTO(i, "Hello World"));
		}
		stopWatch.stop();
		logPerf(singleThreadedTaskName, stopWatch.getTotalTimeMillis(), stopWatch.getTotalTimeSeconds());
	}

	private void logPerf(String taskName, long ms, double sec) {
		log.info("{} : {} ms", taskName, ms);
		log.info("{} : {} sec", taskName, sec);
	}
}
