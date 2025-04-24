package com.example.messaging.common.producer.backup;

import com.example.messaging.common.producer.Producer;

public interface BackupProducer {

	void resend(Producer producer);

	void sendTextMessage(long id, String content);
}
