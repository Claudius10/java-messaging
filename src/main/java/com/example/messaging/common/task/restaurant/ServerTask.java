package com.example.messaging.common.task.restaurant;

import com.example.messaging.common.backup.BackupProvider;
import com.example.messaging.common.exception.producer.ProducerDeliveryException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.task.MessagingTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class ServerTask implements MessagingTask {

	private final CountDownLatch startGate;

	private final CountDownLatch endGate;

	private final BlockingQueue<Dish> completedDishes;

	private final Producer<Dish> producer;

	private final BackupProvider<Dish> backupProvider;

	private final int consumerIdle;

	private final int pollTimeOut;

	private boolean isWorking = false;

	private boolean cancel = false;

	private long timeOfLastDish = 0;

	private long in = 0;

	private long out = 0;

	@Override
	public void run() {
		startWork();
		log.info("Server shift ended");
	}

	private void startWork() {
		try {

			log.info("Waiting on coworkers...");
			startGate.await();
			log.info("All coworkers ready, starting work!");

			while (!Thread.currentThread().isInterrupted()) {

				if (cancel && completedDishes.isEmpty()) {
					producer.close();
					isWorking = false;
					endGate.countDown();
					break;
				}

				Dish dish = completedDishes.poll(pollTimeOut, TimeUnit.MILLISECONDS); // can't wait indefinetly: if cancel becomes true while waiting and chefs went home, it will get stuck

				if (dish != null) {
					isWorking = true;
					timeOfLastDish = System.currentTimeMillis();
					in++;
					serve(dish);
					out++;
				} else {
					handleIdle();
				}
			}
		} catch (InterruptedException ex) {
			log.warn("Server interrupted: {}", ex.getMessage());
			isWorking = false;
			Thread.currentThread().interrupt();
		}
	}

	private void serve(Dish dish) throws InterruptedException {
		if (log.isTraceEnabled()) log.trace("Served {}", dish.getName());
		try {
			producer.send(dish);
		} catch (ProducerDeliveryException ex) {
			backupProvider.send(dish);
		}
	}

	private void handleIdle() throws InterruptedException {
		isWorking = false;
		long elapsed = System.currentTimeMillis() - timeOfLastDish;
		if (elapsed > consumerIdle) {
			if (log.isTraceEnabled()) log.trace("Elapsed time: {} ms", elapsed);
			if (log.isTraceEnabled()) log.trace("Consumer idle: {}", consumerIdle);
			pause();
		}
	}

	private void pause() throws InterruptedException {
		synchronized (completedDishes) {
			if (!cancel) {
				if (log.isTraceEnabled()) log.trace("Waiting for work...");
				completedDishes.wait();
			}
		}
	}

	@Override
	public void cancel() {
		cancel = true;
	}

	@Override
	public boolean isWorking() {
		return isWorking;
	}

	@Override
	public long getInCount() {
		return in;
	}

	@Override
	public long getOutCount() {
		return out;
	}

	@Override
	public long timeInMilisOfLastMessage() {
		return timeOfLastDish;
	}
}
