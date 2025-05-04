package com.example.messaging.jms.backup;

import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.exception.backup.BackupProcessException;
import com.example.messaging.common.exception.backup.BackupReadException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.producer.impl.MyJmsProducer;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class JmsCheckBackup implements BackupCheck {

	private final JmsProperties jmsProperties;

	private final ConnectionFactory connectionFactory;

	private final BackupProvider<Dish> backupProvider;

	private Producer<Dish> producer;

	@Override
	public void backupCheck() {
		processBackedUpMessages();
	}

	private void processBackedUpMessages() {
		log.info("Processing backup...");

		try {
			backupProvider.open();

			if (backupProvider.hasMoreElements()) {
				log.info("Found backed up messages");

				producer = new MyJmsProducer(connectionFactory, jmsProperties);

				if (producer.isConnected()) {

					while (backupProvider.hasMoreElements()) {
						Dish dish = getDish();
						if (dish != null) {
							resend(dish);
						}
					}

					producer.close();

				} else {
					log.info("Unable to proceed with backup processing: producer is not connected");
				}
			} else {
				log.info("No backed up messages found");
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
