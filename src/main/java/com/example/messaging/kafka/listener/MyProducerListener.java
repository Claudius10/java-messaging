package com.example.messaging.kafka.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@Slf4j
public class MyProducerListener implements ProducerListener<Long, String> {

	@Override
	public void onSuccess(ProducerRecord<Long, String> producerRecord, RecordMetadata recordMetadata) {
//		if (log.isTraceEnabled()) log.trace("Acknowledged");
	}

	@Override
	public void onError(ProducerRecord<Long, String> producerRecord, RecordMetadata recordMetadata, Exception exception) {
		log.error("Error delivering record: {}", producerRecord.toString(), exception);
	}
}
