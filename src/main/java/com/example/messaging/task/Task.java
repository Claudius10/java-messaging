package com.example.messaging.task;

public interface Task extends Runnable {

	void cancel();

	boolean isWorking();

	int getInCount();

	int getOutCount();
}
