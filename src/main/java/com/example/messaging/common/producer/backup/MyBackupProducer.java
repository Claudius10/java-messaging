package com.example.messaging.common.producer.backup;

import com.example.messaging.common.producer.Producer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MyBackupProducer implements BackupProducer {

	@Override
	public synchronized void resend(Producer producer) {

	}

	@Override
	public synchronized void sendTextMessage(long id, String content) {

	}
}
