package com.example.messaging.task;

import com.example.messaging.activemq.publisher.JmsPublisher;
import com.example.messaging.activemq.publisher.NoopPublisher;
import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import com.example.messaging.task.async.AsyncToArtemisPublisherTask;
import com.example.messaging.task.sync.ToArtemisPublisherTask;
import com.example.messaging.task.sync.ToSqlPublisherTask;
import com.example.messaging.util.MessageQueue;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@Component
@Slf4j
public class InterceptMessagesTask {

	@Value("${messages}")
	private int amountToProduce;

	private final ThreadPoolTaskExecutor taskExecutor;

	private final MessageQueue messageQueue;

	private final JmsTemplate jmsTemplate;

	public InterceptMessagesTask(
			@Qualifier("activeMQTaskExecutor")
			ThreadPoolTaskExecutor taskExecutor,
			MessageQueue messageQueue,
			JmsTemplate jmsTemplate) {
		this.taskExecutor = taskExecutor;
		this.messageQueue = messageQueue;
		this.jmsTemplate = jmsTemplate;
	}

	@PostConstruct
	public void run() {
		StopWatch stopWatch = new StopWatch("JMS Tasks");

		// book Java Concurrency in Practice
		// https://bytebytego.com/guides/blocking-vs-non-blocking-queue
		// https://www.baeldung.com/java-concurrent-queues
		// https://www.baeldung.com/java-producer-consumer-problem
		// https://java-design-patterns.com/patterns/producer-consumer/#related-java-design-patterns
		// https://www.geeksforgeeks.org/producer-consumer-solution-using-threads-java/
		// https://www.geeksforgeeks.org/producer-consumer-solution-using-blockingqueue-in-java-thread/

		List<Message> messages = produceMessages(amountToProduce);

		Publisher jmsPublisher = new JmsPublisher(jmsTemplate);
		Publisher sqlPublisher = new NoopPublisher();

		//stopWatch.start("ToArtemisPublisherTask");
		//toArtemisPublisher(messages, jmsPublisher);
		//stopWatch.stop();

		stopWatch.start("AsyncToArtemisPublisherTask");
		asyncToArtemisPublisher(messages, jmsPublisher);
		stopWatch.stop();
		log.info(stopWatch.prettyPrint());


//		ToSqlPublisherTask toSqlPublisherTask = new ToSqlPublisherTask(sqlPublisher, messageQueue, stopWatch);
//		toSqlPublisherTask.run();

//		activemqTaskExecutor.execute(new AsyncToArtemisPublisherTask(messages, new JmsPublisher(jmsTemplate), stopWatch));

		taskExecutor.execute(new ToSqlPublisherTask(sqlPublisher, messageQueue));
	}


	private void toArtemisPublisher(List<Message> messages, Publisher publisher) {
		ToArtemisPublisherTask toArtemisPublisherTask = new ToArtemisPublisherTask(messages, publisher);
		toArtemisPublisherTask.run();
	}

	private void asyncToArtemisPublisher(List<Message> messages, Publisher publisher) {
		// A Future represents the result of an asynchronous computation.
		// list to add the results of all tasks
		List<Future<?>> futures = new ArrayList<>();

		for (Message message : messages) {
			Future<?> taskResult = taskExecutor.submit(new AsyncToArtemisPublisherTask(publisher, message));
			futures.add(taskResult);
		}

		// future.get() Waits if necessary for the computation to complete, and then retrieves its result.
		// therefore, if future.get() is called on all futures, then the stopwatch is stopped after all tasks have been completed
		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private List<Message> produceMessages(int amount) {
		List<Message> messages = new ArrayList<>();
		for (int i = 0; i < amountToProduce; i++) {
			Message message = Message.builder()
					.withContent("Hello World " + i)
					.build();
			messages.add(message);
		}

		return messages;
	}
}