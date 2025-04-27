package com.example.messaging.jms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("Jms")
@Configuration
@ConfigurationProperties(prefix = "jms")
@Getter
@Setter
public class JmsProperties {

	private String brokerUrl;

	private String user;

	private String password;

	private String producer;

	private String consumerClientId;

	private String destination;

	private int maxConnections;

	private int pollTimeOut;

	private int reconnectionIntervalMs;

	private int reconnectionMaxAttempts;
}