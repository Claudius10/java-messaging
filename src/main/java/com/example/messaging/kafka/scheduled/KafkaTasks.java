package com.example.messaging.kafka.scheduled;

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
public class KafkaTasks {

	private final KafkaProperties kafkaProperties;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final MyKafkaAdmin myKafkaAdmin;

	private final BackupProvider<Dish> backupProvider;

	public int getBackupCheckInterval() {
		return kafkaProperties.getBackupCheckInterval();
	}

	public void processBackedUpMessages() {
		if (log.isTraceEnabled()) log.trace("Processing backup...");

		try {
			backupProvider.open();

			if (backupProvider.hasMoreElements()) {
				if (log.isTraceEnabled()) log.trace("Found backed up messages");

				Producer<Dish> producer = new MyKafkaProducer(kafkaProperties.getTopic(), kafkaTemplate, myKafkaAdmin);

				if (producer.isConnected()) {

					while (backupProvider.hasMoreElements()) {
						Dish dish;
						try {
							dish = backupProvider.read();
							try {
								if (log.isTraceEnabled()) log.trace("Resending dish {}", dish.getName());
								producer.sendTextMessage(dish);
							} catch (Exception ex) {
								backupProvider.onFailure(dish);
							}
						} catch (BackupReadException ex) {
							log.error("Unable to read backed up message {}", ex.getMessage());
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
}
