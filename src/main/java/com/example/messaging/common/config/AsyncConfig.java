package com.example.messaging.common.config;

import com.example.messaging.common.util.RestaurantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class AsyncConfig {

	private final RestaurantProperties restaurantProperties;

	@Bean
	TaskExecutor workers() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("worker-");
		final int producersAndConsumersPair = restaurantProperties.getMaxConnections() * 2;
		taskExecutor.setCorePoolSize(producersAndConsumersPair);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}
}
