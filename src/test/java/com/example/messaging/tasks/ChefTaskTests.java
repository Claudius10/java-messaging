package com.example.messaging.tasks;

import com.example.messaging.customer.Customer;
import com.example.messaging.customer.MyCustomer;
import com.example.messaging.model.Dish;
import com.example.messaging.task.ChefTask;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

public class ChefTaskTests {

	@Test
	void givenStart_thenGreetCustomer() throws InterruptedException {

		// Arrange

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		BlockingQueue<Dish> dishes = new LinkedBlockingQueue<>();
		int dishesToProduce = 1;
		int greetTimeOutSeconds = 2;

		Customer customer = new MyCustomer(dishesToProduce);
		ChefTask chefTask = new ChefTask(countDownLatch, end, dishes, customer, greetTimeOutSeconds);
		Thread chefThread = new Thread(chefTask);

		// Acts

		chefThread.start(); // start chef thread
		countDownLatch.countDown(); // remove barrier

		// Assert

		int waitMilis = 100;
		Thread.sleep(waitMilis); // wait for chef to cook the dish (even though highly unlikely to fail, if it does, increase waitMilis)
		assertThat(chefTask.getInCount()).isEqualTo(dishesToProduce);
		assertThat(chefTask.getOutCount()).isEqualTo(dishesToProduce);
	}

	@Test
	void givenIdle_whenGreetTimeOutTimeDidNotElapse_thenDoNotGreetCustomer() throws InterruptedException {

		// Arrange

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(1);
		BlockingQueue<Dish> dishes = new LinkedBlockingQueue<>();
		int dishesToProduce = 1;
		int greetTimeOutSeconds = 60;

		Customer customer = new MyCustomer(dishesToProduce);
		ChefTask chefTask = new ChefTask(countDownLatch, end, dishes, customer, greetTimeOutSeconds);
		Thread chefThread = new Thread(chefTask);

		// Act

		chefThread.start(); // start chef thread
		countDownLatch.countDown(); // remove barrier

		// Assert

		int waitMilis = 100;
		Thread.sleep(waitMilis);
		assertThat(chefTask.getInCount()).isEqualTo(dishesToProduce);
		assertThat(chefTask.getOutCount()).isEqualTo(dishesToProduce);

		// to refute/falsify this test, set waitMilis higher than greetTimeOutSeconds
	}
}
