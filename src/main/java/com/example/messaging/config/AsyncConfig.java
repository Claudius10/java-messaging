package com.example.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	@Bean
	ThreadPoolTaskExecutor producerThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("producer-");
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setQueueCapacity(25);
		return taskExecutor;
	}

	@Bean
	ThreadPoolTaskExecutor consumerThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("consumer-");
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setQueueCapacity(25);
		return taskExecutor;
	}
}
