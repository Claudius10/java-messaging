package com.example.messaging.task.async;

public interface Task extends Runnable {

	void cancel();

	boolean isWorking();

	int getInCount();

	int getOutCount();
}
