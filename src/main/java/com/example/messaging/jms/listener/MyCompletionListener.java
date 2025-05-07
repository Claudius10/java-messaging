package com.example.messaging.jms.listener;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.util.Constants;
import jakarta.jms.CompletionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MyCompletionListener implements CompletionListener {

	private final BackupProvider<Dish> dishBackupProvider;

	private final ProducerMetrics producerMetrics;

	@Override
	public void onCompletion(Message message) {
		String content = getMessageText(message);

		if (isTestRequest(content)) {
			if (log.isTraceEnabled()) log.trace("Ignored test request");
			return;
		}

		long sent = producerMetrics.sent();
		if (log.isTraceEnabled()) log.trace("Broker received message '{}' with sentId '{}'", content, sent);
	}

	@Override
	public void onException(Message message, Exception ex) {
		// in case it ever lands here instead of the thread that sends, for whatever reason
		String content = getMessageText(message);

		if (isTestRequest(content)) {
			if (log.isTraceEnabled()) log.trace("Ignored test request");
			return;
		}

		long error = producerMetrics.error();
		log.error("Failed to send message '{}' with errorId '{}' to broker: '{}'", content, error, ex.getMessage(), ex);

		TextMessage textMessage = (TextMessage) message;

		try {
			Dish dish = new Dish();
			dish.setName(textMessage.getText());
			dish.setId(Long.valueOf(textMessage.getStringProperty("id")));
			dish.setCooked(true);
			dishBackupProvider.send(dish);
		} catch (JMSException e) {
			log.error("Failed to parse dish in order to proceed with back up: '{}'", ex.getMessage());
			// send notification
			// send email
			// call the police
			// ...
		}
	}

	private String getMessageText(Message message) {
		try {
			return ((TextMessage) message).getText();
		} catch (JMSException ex) {
			log.error("Failed to parse Message: '{}'", ex.getMessage());
			return null;
		}
	}

	private boolean isTestRequest(String content) {
		if (content == null) {
			return false;
		}
		return content.equals(Constants.TEST_REQUEST);
	}
}