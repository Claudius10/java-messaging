package com.example.messaging.common.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "restaurant")
@Getter
@Setter
public class RestaurantProperties {

	private int dishesToProduce;

	private int dishesQueueCapacity;

	private int maxConnections;

	private int producerIdle;

	private int consumerIdle;
}
