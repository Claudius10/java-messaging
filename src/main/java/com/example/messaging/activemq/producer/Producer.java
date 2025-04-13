package com.example.messaging.activemq.producer;

import com.example.messaging.model.Message;
import com.example.messaging.util.ExternalInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
@Slf4j
public class Producer implements Runnable {

	boolean running = false;

	private final ExternalInput externalInput;

	private final BlockingQueue<Message> messageQueue;

	@Override
	public void run() {
		log.info("Producer started");
		running = true;
		produce();
	}

	private void produce() {
		while (running) {
			Message message = externalInput.read();

			if (message != null) {
				log.info("Message received: {}", message.getContent());

				try {
					messageQueue.put(message);
				} catch (InterruptedException ex) {
					log.error("Interrupted", ex);
				}
			}
		}
	}

	public void stop() {
		log.info("Producer stopped");
		running = false;
	}
}
