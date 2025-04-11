package com.example.messaging.task.sync;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import com.example.messaging.util.MessageQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ToSqlPublisherTask implements Runnable {

	private final Publisher publisher;

	private final MessageQueue messageQueue;

	@Override
	public void run() {
		try {

			Message message = messageQueue.poll();

			while (message != null) {
				boolean success = true;

				try {
					publisher.publishMessage(message);
				} catch (Exception e) {
					log.error("Publishing message failed", e);
					success = false;
				}

				if (success) {
					message = messageQueue.poll();
				}
			}

		} catch (InterruptedException e) {
			log.error("Publishing message failed", e);
			throw new RuntimeException(e);
		}

	}
}
