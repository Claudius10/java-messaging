package com.example.messaging.task.async;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class AsyncToArtemisPublisherTask implements Runnable {

	private final Publisher publisher;

	private final Message message;

	@Override
	public void run() {
		publisher.publishMessage(message);
	}
}
