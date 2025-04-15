package com.example.messaging.task.async;

import com.example.messaging.model.Customer;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static com.example.messaging.util.ExceptionUtils.launderThrowable;

public class GreetCustomerTask {

	private final FutureTask<Customer> potentialCustomer = new FutureTask<>(this::showMenu);

	private final Thread thread = new Thread(potentialCustomer);

	public void start() {
		thread.start();
	}

	public Customer greetCustomer() throws CustomerGreetException, InterruptedException {
		try {
			return potentialCustomer.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CustomerGreetException) {
				throw (CustomerGreetException) cause;
			} else {
				throw launderThrowable(cause);
			}
		}
	}

	private Customer showMenu() {
		if (getCustomerResponse()) {
			return new Customer();
		} else {
			throw new CustomerGreetException("Sorry, but there's nothing fancy on the menu. Maybe next time.");
		}
	}

	private boolean getCustomerResponse() {
		Random random = new Random();
		return random.nextBoolean();
	}
}
