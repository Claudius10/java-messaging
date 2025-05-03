package com.example.messaging.kafka.producer.impl;

import com.example.messaging.common.exception.producer.ProducerDeliveryException;
import com.example.messaging.common.exception.producer.ProducerSendException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.kafka.admin.MyKafkaAdmin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class MyKafkaProducer implements Producer<Dish> {

	private final String destination;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final MyKafkaAdmin myKafkaAdmin;

	private CompletableFuture<SendResult<Long, String>> pending;

	@Override
	public void sendTextMessage(Dish dish) throws ProducerDeliveryException {

		Long id = dish.getId();
		String content = dish.getName();

		try {
			pending = kafkaTemplate.send(destination, id, content);
		} catch (KafkaException ex) {
			log.warn("Failed to send message {} to destination {}: {}", content, destination, ex.getMessage());
			throw new ProducerSendException();
		}
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing Kafka Producer...");

		logMetrics();

		if (pending == null) {
			kafkaTemplate.getProducerFactory().closeThreadBoundProducer();
			return;
		}

		pending.whenComplete((result, ex) -> {
			kafkaTemplate.getProducerFactory().closeThreadBoundProducer();
		});
	}

	private void logMetrics() {
		Map<MetricName, ? extends Metric> metrics = kafkaTemplate.metrics();

		for (Map.Entry<MetricName, ? extends Metric> entry : metrics.entrySet()) {
			if (entry.getKey().name().equals("record-send-total") || entry.getKey().name().equals("record-send-rate")) {
				log.info("Send -> {} - {} -> {}", entry.getKey().name(), entry.getKey().tags(), entry.getValue().metricValue());
			}

			if (entry.getKey().name().equals("record-retry-total")) {
				log.info("Retry -> {} - {} -> {}", entry.getKey().name(), entry.getKey().tags(), entry.getValue().metricValue());
			}

			if (entry.getKey().name().equals("record-error-total")) {
				log.info("Error -> {} - {} -> {}", entry.getKey().name(), entry.getKey().tags(), entry.getValue().metricValue());
			}
		}
	}

	@Override
	public boolean isConnected() {
		try {
			return myKafkaAdmin.clusterId() != null;
		} catch (KafkaException ex) {
			return false;
		}
	}
}
