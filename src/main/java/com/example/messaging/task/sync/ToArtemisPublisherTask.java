package com.example.messaging.task.sync;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class ToArtemisPublisherTask {

	private final List<Message> messages;

	private final Publisher publisher;

	public void run() {
		log.info("Sending {} messages to Artemis", messages.size());
		for (Message message : messages) {
			publisher.publishMessage(message);
		}
	}
}
