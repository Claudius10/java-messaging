package com.example.messaging.util;

import com.example.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
public class ExternalInput {

	private final List<Message> messages;

	public ExternalInput(int amount) {
		messages = new ArrayList<>(produceMessages(amount));
	}

	public Message read() {
		Message message = null;

		try {
			message = messages.getFirst();
			messages.removeFirst();
		} catch (NoSuchElementException ex) {
			// ignore
		}

		return message;
	}

	private List<Message> produceMessages(int amount) {
		List<Message> messages = new ArrayList<>();

		for (long i = 0; i < amount; i++) {
			Message message = Message.builder()
					.withContent("Hello World " + i)
					.withId(i)
					.build();
			messages.add(message);
		}

		return messages;
	}
}
