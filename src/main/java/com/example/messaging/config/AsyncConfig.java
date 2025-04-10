package com.example.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	@Bean
	ThreadPoolTaskExecutor publisherTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(8);
		taskExecutor.setMaxPoolSize(8);
		taskExecutor.setQueueCapacity(10000);
		return taskExecutor;
	}
}
