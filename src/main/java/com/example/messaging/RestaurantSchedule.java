package com.example.messaging;

import com.example.messaging.restaurant.Restaurant;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 1x: Main Thread
 * 1x: MyCompletionListener
 * 3x: JmsConsumer
 * 3x: Producer
 * 3x: Consumer
 * 1x: roaming
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantSchedule {

	private final ConfigurableApplicationContext restaurantControls;

	private final Restaurant myJmsRestaurant;

	@PostConstruct
	public void openJmsRestaurant() {
		myJmsRestaurant.open();
	}

	@Scheduled(initialDelay = 30, timeUnit = TimeUnit.SECONDS)
	public void closeJmsRestaurant() {
		try {
			myJmsRestaurant.close();
			restaurantControls.close();
		} catch (InterruptedException e) {
			log.error("Interrupted with closing JMS Restaurant: {}", e.getMessage());
		}
	}
}