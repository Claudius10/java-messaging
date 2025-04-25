package com.example.messaging.jms.producer.impl;

import com.example.messaging.common.exception.BackupProcessException;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.producer.backup.BackupProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("Jms")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyJmsBackupProducer implements BackupProducer {

	@Override
	public synchronized void resend(Producer producer) throws BackupProcessException {

	}

	@Override
	public synchronized void sendTextMessage(long id, String content) {
		if (log.isTraceEnabled()) log.trace("Backed up message {}", content);
	}
}
