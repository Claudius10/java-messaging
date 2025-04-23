package com.example.messaging.kafka.producer;

import com.example.messaging.common.producer.Producer;
import org.springframework.kafka.core.KafkaTemplate;

public interface KafkaProducer extends Producer {

	KafkaTemplate<Long, String> getTemplate();
}
