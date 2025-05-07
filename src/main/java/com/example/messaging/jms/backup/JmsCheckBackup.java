package com.example.messaging.jms.backup;

import com.example.messaging.common.backup.BackupCheck;
import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.exception.backup.BackupProcessException;
import com.example.messaging.common.exception.backup.BackupReadException;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.jms.config.JmsProperties;
import com.example.messaging.jms.listener.MyCompletionListener;
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

	private final ProducerMetrics producerMetrics;

	private final BackupProvider<Dish> backupProvider;

	private Producer<Dish> producer;

	@Override
	public void backupCheck() {
		log.info("Processing backup...");

		try {
			backupProvider.open();

			if (backupProvider.hasMoreElements()) {
				producer = new MyJmsProducer(connectionFactory, jmsProperties, producerMetrics, new MyCompletionListener(backupProvider, producerMetrics));

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

			cleanUp();
		} catch (BackupProcessException ex) {
			log.error("Backup processing failed '{}'", ex.getMessage());
			cleanUp();
		}
	}

	private void cleanUp() {
		backupProvider.close();
		producer.close();
		producerMetrics.print();
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
			log.error("Unable to read backed up message: '{}'", ex.getMessage());
		}

		return dish;
	}
}
