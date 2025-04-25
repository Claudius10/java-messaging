package com.example.messaging.kafka.producer.impl;

import com.example.messaging.common.exception.BackupProcessException;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.producer.backup.BackupProducer;
import com.example.messaging.kafka.admin.MyKafkaAdmin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyKafkaBackupProducer implements BackupProducer {

	private final MyKafkaAdmin myKafkaAdmin;

	@Override
	public synchronized void resend(Producer producer) {
		try {
			if (myKafkaAdmin.clusterId() != null) {
				// process
			}
		} catch (KafkaException ex) {
			throw new BackupProcessException(ex.getMessage());
		}
	}

	@Override
	public synchronized void sendTextMessage(long id, String content) {
		if (log.isTraceEnabled()) log.trace("Backed up message {}", content);
	}
}
