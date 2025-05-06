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
		long sent = producerMetrics.sent();
		if (log.isTraceEnabled()) log.trace("Broker received message '{}' with sentId '{}' in topic '{}:{}'", record.value(), sent, recordMetadata.topic(), record.partition());
	}

	@Override
	public void onError(ProducerRecord<Long, String> record, RecordMetadata recordMetadata, Exception ex) {
		long error = producerMetrics.error();
		log.error("Failed to send message '{}' with errorId '{}' to destination '{}:{}': {}", record.value(), error, record.topic(), record.partition(), ex.getMessage());
		Dish dish = Dish.builder().withId(record.key()).withName(record.value()).withCooked(true).build();
		dishBackupProvider.send(dish);
	}
}
