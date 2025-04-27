package com.example.messaging.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AsyncConfig {

	@Bean
	ThreadPoolTaskScheduler workers() {
		ThreadPoolTaskScheduler taskExecutor = new ThreadPoolTaskScheduler();
		taskExecutor.setThreadNamePrefix("worker-");
		taskExecutor.setPoolSize(6);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}
}
