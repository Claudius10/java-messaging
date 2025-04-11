package com.example.messaging.util;

import com.example.messaging.model.Message;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class MessageQueue {

	@Value("${queue.messages.capacity}")
	private int capacity;

	@Value("${queue.messages.delay}")
	private int delay;

	private BlockingQueue<Message> messageQueue;

	@PostConstruct
	private void init() {
		this.messageQueue = new ArrayBlockingQueue<>(capacity);
	}

	public boolean offer(Message message) {
		return messageQueue.offer(message);
	}

	public Message poll() throws InterruptedException {
		return messageQueue.poll(delay, TimeUnit.SECONDS);
	}
}
