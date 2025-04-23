package com.example.messaging.common.task;

public interface MessagingTask extends Task {

	long getInCount();

	long getOutCount();

	long timeInMilisOfLastMessage();

	boolean isWorking();
}
