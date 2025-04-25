package com.example.messaging.common.producer.backup;

import com.example.messaging.common.exception.BackupProcessException;
import com.example.messaging.common.producer.Producer;

public interface BackupProducer {

	void resend(Producer producer) throws BackupProcessException;

	void sendTextMessage(long id, String content);
}
