package com.example.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	@Bean
	ThreadPoolTaskExecutor chefs() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("chef-");
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setQueueCapacity(25);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}

	@Bean
	ThreadPoolTaskExecutor servers() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("server-");
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setQueueCapacity(25);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}
}
