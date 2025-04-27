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

	private int apiTimeoutMs;

	private String clientId;

	private String producerClientId;

	private int producerTimeOutMs;

	private int producerBlockMs;

	private String producerAckMode;

	private String producer;

	private String consumerClientId;

	private int consumerTimeoutMs;

	private String consumerGroupId;

	private String topic;

	private int maxConnections;

	private int pollTimeOut;
}
