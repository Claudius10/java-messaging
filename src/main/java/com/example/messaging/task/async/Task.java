package com.example.messaging.task.async;

import java.util.concurrent.Callable;

public interface Task extends Callable<Boolean> {

	void cancel();
}
