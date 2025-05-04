package com.example.messaging.kafka.config;

import com.example.messaging.kafka.listener.MyProducerListener;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;
import java.util.Map;

@Profile("Kafka")
@Configuration
@RequiredArgsConstructor
@EnableKafka
public class KafkaConfig {

	private final KafkaProperties kafkaProperties;

	// Consumer

	@Bean
	KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Long, String>> kafkaListenerContainerFactory(ConsumerFactory<Long, String> consumerFactory) {
		ConcurrentKafkaListenerContainerFactory<Long, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setConcurrency(kafkaProperties.getMaxConnections()); // only for the consumers
		factory.getContainerProperties().setPollTimeout(kafkaProperties.getPollTimeOut());
		factory.setAutoStartup(false);
		return factory;
	}

	@Bean
	ConsumerFactory<Long, String> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigs());
	}

	private Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProperties.getConsumerClientId());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return props;
	}

	// Producer

	@Bean
	KafkaTemplate<Long, String> kafkaTemplate(ProducerFactory<Long, String> producerFactory) {
		KafkaTemplate<Long, String> template = new KafkaTemplate<>(producerFactory);
		template.setProducerListener(new MyProducerListener());
		return template;
	}

	@Bean
	public ProducerFactory<Long, String> producerFactory() {
		DefaultKafkaProducerFactory<Long, String> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs());
		producerFactory.setProducerPerThread(true); // does not affect performance, but it does affect safety!!!
		return producerFactory;
	}

	// https://kafka.apache.org/documentation/#producerconfigs
	private Map<String, Object> producerConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerUrl());
		props.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getProducerClientId());
		props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducerAckMode());
		props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, kafkaProperties.getProducerBlockMs()); // ms to wait before throwing when attempting to send
		props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaProperties.getProducerTimeOutMs()); // ms between producer connection attempts
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return props;
	}

	@Bean
	public NewTopic topic() {
		return TopicBuilder.name(kafkaProperties.getTopic())
				.partitions(kafkaProperties.getMaxConnections())
				.replicas(1)
				.build();
	}
}
