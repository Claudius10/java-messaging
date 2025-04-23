package com.example.messaging.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("Kafka")
@Configuration
@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
public class KafkaProperties {

	private String brokerUrl;

	private String topic;

	private int maxConnections;

	private int pollTimeOut;

	private String clientId;

	private String consumerId;

	private String consumerGroupId;

	private String producerClientId;

	private int producerRetries;

	private String producerAckMode;

	private String producer;
}
