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

	private final String topic;

	private final KafkaTemplate<Long, String> kafkaTemplate;

	private final MyKafkaAdmin myKafkaAdmin;

	private CompletableFuture<SendResult<Long, String>> sendFuture;

	@Override
	public void sendTextMessage(Dish dish) throws ProducerDeliveryException {
		try {
			sendFuture = kafkaTemplate.send(topic, dish.getId(), dish.getName());
		} catch (KafkaException ex) {
			log.warn("Unable to send message: {}", ex.getMessage());
			throw new ProducerSendException();
		}
	}

	@Override
	public void close() {
		if (log.isTraceEnabled()) log.trace("Closing Kafka Producer...");

		logMetrics();

		if (sendFuture == null) {
			kafkaTemplate.getProducerFactory().closeThreadBoundProducer();
			return;
		}

		sendFuture.whenComplete((result, ex) -> {
			kafkaTemplate.getProducerFactory().closeThreadBoundProducer();
		});
	}

	@Override
	public boolean isConnected() {
		return myKafkaAdmin.clusterId() != null;
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
}
