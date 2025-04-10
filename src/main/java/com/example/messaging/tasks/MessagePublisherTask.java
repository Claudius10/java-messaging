package com.example.messaging.tasks;

import com.example.messaging.contracts.Publisher;
import com.example.messaging.model.JmsTextMessageDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class MessagePublisherTask implements Runnable {

	private final Publisher publisher;
	private List<JmsTextMessageDTO> messageList;

	@Override
	public void run() {
		int i = 0;

		while (i < messageList.size()) {
			sendMessage(messageList.get(i));
			i++;
		}

		messageList.clear();
		messageList = null;
		log.info("Message publisher task finished");
	}

	private void sendMessage(final JmsTextMessageDTO message) {
		publisher.publishMessage(message);
	}
}
