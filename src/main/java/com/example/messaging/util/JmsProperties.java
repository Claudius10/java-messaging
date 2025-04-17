package com.example.messaging.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jms")
@Getter
@Setter
public class JmsProperties {

	private String brokerUrl;

	private String user;

	private String password;

	private String maxConnections;

	private String destination;

	private String factory;
}