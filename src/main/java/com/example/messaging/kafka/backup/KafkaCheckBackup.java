package com.example.messaging.kafka.backup;

import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.exception.backup.BackupProcessException;
import com.example.messaging.common.exception.backup.BackupReadException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.kafka.admin.MyKafkaAdmin;
import com.example.messaging.kafka.config.KafkaProperties;
import com.example.messaging.kafka.producer.impl.MyKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCheckBackup implements BackupCheck {

	private final KafkaProperties kafkaProperties;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final MyKafkaAdmin myKafkaAdmin;

	private final BackupProvider<Dish> backupProvider;

	private Producer<Dish> producer;

	@Override
	public void backupCheck() {
		processBackedUpMessages();
	}

	private void processBackedUpMessages() {
		if (log.isTraceEnabled()) log.trace("Processing backup...");

		try {
			backupProvider.open();

			if (backupProvider.hasMoreElements()) {
				if (log.isTraceEnabled()) log.trace("Found backed up messages");

				producer = new MyKafkaProducer(kafkaProperties.getTopic(), kafkaTemplate, myKafkaAdmin);

				if (producer.isConnected()) {

					while (backupProvider.hasMoreElements()) {
						Dish dish = getDish();
						if (dish != null) {
							resend(dish);
						}
					}

					producer.close();

				} else {
					if (log.isTraceEnabled()) log.trace("Unable to proceed with backup processing: producer is not connected");
				}
			} else {
				if (log.isTraceEnabled()) log.trace("No backed up messages found");
			}

			backupProvider.close();
		} catch (BackupProcessException ex) {
			log.error("Backup processing failed {}", ex.getMessage());
		}
	}

	private Dish getDish() {
		Dish dish = null;

		try {
			dish = backupProvider.read();
		} catch (BackupReadException ex) {
			log.error("Unable to read backed up message {}", ex.getMessage());
		}

		return dish;
	}

	private void resend(Dish dish) {
		try {
			if (log.isTraceEnabled()) log.trace("Resending dish {}", dish.getName());
			producer.sendTextMessage(dish);
		} catch (Exception ex) {
			backupProvider.onFailure(dish);
		}
	}
}
