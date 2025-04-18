package com.example.messaging.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "restaurant")
@Getter
@Setter
public class RestaurantProperties {

	private int dishes;

	private int dishesCapacity;

	private int takeGiveUp;

	private int maxCapacity;

	private int greetTimeOut;
}
