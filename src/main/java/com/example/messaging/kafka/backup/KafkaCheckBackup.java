package com.example.messaging.kafka.backup;

import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.exception.backup.BackupProcessException;
import com.example.messaging.common.exception.backup.BackupReadException;
import com.example.messaging.common.metrics.ProducerMetrics;
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

	private final ProducerMetrics producerMetrics;

	private final BackupProvider<Dish> backupProvider;

	private Producer<Dish> producer;

	@Override
	public void backupCheck() {
		log.info("Processing backup...");

		try {
			backupProvider.open();

			if (backupProvider.hasMoreElements()) {
				producer = new MyKafkaProducer(kafkaProperties.getTopic(), kafkaTemplate, myKafkaAdmin);

				if (producer.isConnected()) {

					while (backupProvider.hasMoreElements()) {
						Dish dish = getDish();
						if (dish != null) {
							resend(dish);
						}
					}

				} else {
					log.info("Unable to proceed with backup processing: producer is not connected");
				}
			} else {
				log.info("No backed up messages found");
			}

			backupProvider.close();
			// wait to receive all acks from broker
			final int waitAcks = kafkaProperties.getProducerBlockMs() + 500;
			Thread.sleep(waitAcks);
			producer.close();
			producerMetrics.print();
		} catch (BackupProcessException ex) {
			log.error("Backup processing failed {}", ex.getMessage());
		} catch (InterruptedException ex) {
			log.error("Backup processing interrupted {}", ex.getMessage());
		}
	}

	private void resend(Dish dish) {
		try {
			producer.send(dish);
			long resent = producerMetrics.resent();
			if (log.isTraceEnabled()) log.trace("Resent dish '{}' with resentId '{}'", dish.getName(), resent);
		} catch (Exception ex) {
			backupProvider.onFailure(dish);
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
}
