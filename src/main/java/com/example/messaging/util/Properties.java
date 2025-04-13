package com.example.messaging.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Properties {

	@Value("${messages}")
	private int orders;

	@Value("${queue.messages.capacity}")
	private int queueCapacity;

	@Value("${queue.messages.giveUp}")
	private int queueGiveUpDelay;

	@Value("${external.input.pool}")
	private int externalInputPool;
}
