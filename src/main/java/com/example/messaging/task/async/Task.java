package com.example.messaging.task.async;

public interface Task extends Runnable {

	void cancel();
}
