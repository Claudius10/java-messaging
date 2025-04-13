package com.example.messaging;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.activemq.consumer.Consumer;
import com.example.messaging.activemq.producer.Producer;
import com.example.messaging.model.Message;
import com.example.messaging.util.ExternalInput;
import com.example.messaging.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Main {

	private final Properties properties;

	private final Publisher jmsQueuePublisher;

	private final ThreadPoolTaskExecutor producerThreadPoolTaskExecutor;

	private final ThreadPoolTaskExecutor consumerThreadPoolTaskExecutor;

	@Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void execute() {
		jmsQueuePublisher.setDestination(new ActiveMQQueue("testQueue"));

		int connectionCount = 3;
		List<ExternalInput> externalInputs = new ArrayList<>();
		for (int i = 0; i < connectionCount; i++) {
			externalInputs.add(new ExternalInput(properties.getAmountToProduce()));
		}

		externalInputs.forEach(input -> {
			BlockingQueue<Message> messageQueue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
			producerThreadPoolTaskExecutor.execute(new Producer(input, messageQueue));
			consumerThreadPoolTaskExecutor.execute(new Consumer(messageQueue, jmsQueuePublisher, properties.getQueueGiveUpDelay()));
		});
	}

//	private void asyncToArtemisPublisher(List<Message> messages, Publisher publisher) {
//		// A Future represents the result of an asynchronous computation.
//		// list to add the results of all tasks
//		List<Future<?>> futures = new ArrayList<>();
//
//		for (Message message : messages) {
//			Future<?> taskResult = taskExecutor.submit(new MainTask(publisher, message));
//			futures.add(taskResult);
//		}
//
//		// future.get() Waits if necessary for the computation to complete, and then retrieves its result.
//		// therefore, if future.get() is called on all futures, then the stopwatch is stopped after all tasks have been completed
//		try {
//			for (Future<?> future : futures) {
//				future.get();
//			}
//		} catch (Exception e) {
//			log.error(e.getMessage());
//		}
//	}
}