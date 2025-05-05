package com.example.messaging.kafka.listener;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.metrics.ProducerMetrics;
import com.example.messaging.common.model.Dish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

@Profile("Kafka")
@Component
@RequiredArgsConstructor
@Slf4j
public class MyProducerListener implements ProducerListener<Long, String> {

	private final BackupProvider<Dish> dishBackupProvider;

	private final ProducerMetrics producerMetrics;

	@Override
	public void onSuccess(ProducerRecord<Long, String> record, RecordMetadata recordMetadata) {
		producerMetrics.sent();
		if (log.isTraceEnabled()) log.trace("Sent message '{}' to destination '{}'", record.value(), recordMetadata.topic());
	}

	@Override
	public void onError(ProducerRecord<Long, String> record, RecordMetadata recordMetadata, Exception ex) {
		producerMetrics.error();
		log.error("Failed to send message '{}' to destination '{}': {}", record.value(), record.topic(), ex.getMessage());
		Dish dish = Dish.builder().withId(record.key()).withName(record.value()).withCooked(true).build();
		dishBackupProvider.send(dish);
	}
}
