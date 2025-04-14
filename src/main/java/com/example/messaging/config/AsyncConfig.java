package com.example.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AsyncConfig {

	@Bean
	ThreadPoolTaskExecutor threadPool() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setThreadNamePrefix("thread-");
		taskExecutor.setCorePoolSize(8);
		taskExecutor.setMaxPoolSize(8);
		taskExecutor.setQueueCapacity(25);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}

	@Bean
	ThreadPoolTaskScheduler scheduler() {
		ThreadPoolTaskScheduler taskExecutor = new ThreadPoolTaskScheduler();
		taskExecutor.setThreadNamePrefix("scheduler-");
		taskExecutor.setPoolSize(2);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}
}
