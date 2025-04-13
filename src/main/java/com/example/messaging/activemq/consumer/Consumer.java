package com.example.messaging.activemq.consumer;

import com.example.messaging.activemq.Publisher;
import com.example.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.JmsException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class Consumer implements Runnable {

	boolean running = false;

	private final BlockingQueue<Message> messageQueue;

	private final Publisher publisher;

	private final int delay;

	@Override
	public void run() {
		log.info("Consumer started");
		running = true;
		consume();
	}

	private void consume() {
		while (running) {
			try {
				Message message = messageQueue.poll(delay, TimeUnit.SECONDS);

				while (message != null) {
					boolean ok = true;

					try {
						publisher.publishMessage(message);
					} catch (JmsException ex) {
						log.error("Publishing message failed", ex);
						ok = false;
					}

					if (ok) {
						message = messageQueue.poll();
					}
				}
			} catch (InterruptedException ex) {
				log.error("Consumer interrupted {}", ex.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}

	public void stop() {
		log.info("Consumer stopped");
		running = false;
	}
}
