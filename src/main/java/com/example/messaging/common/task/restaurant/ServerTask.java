package com.example.messaging.common.task.restaurant;

import com.example.messaging.common.exception.BackupProcessException;
import com.example.messaging.common.exception.ProducerDeliveryException;
import com.example.messaging.common.model.Dish;
import com.example.messaging.common.producer.Producer;
import com.example.messaging.common.producer.backup.BackupProducer;
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

	private final Producer producer;

	private final BackupProducer backupProducer;

	private final int consumerIdle;

	private final int pollTimeOut;

	private long in = 0;

	private long out = 0;

	private boolean isWorking = false;

	private boolean cancel = false;

	private long timeOfLastDish = 0;

	@Override
	public void run() {
		startWork();
	}

	private void startWork() {
		try {
			if (log.isTraceEnabled()) log.trace("Waiting on coworkers...");
			startGate.await();
			if (log.isTraceEnabled()) log.trace("All coworkers ready, starting work!");
			isWorking = true;
			while (!Thread.currentThread().isInterrupted()) {

				if (cancel && completedDishes.isEmpty()) {
//					producer.close();
					isWorking = false;
					endGate.countDown();
					if (log.isTraceEnabled()) log.trace("Server shift ended");
					break;
				}

				Dish dish = completedDishes.poll(pollTimeOut, TimeUnit.MILLISECONDS); // can't wait indefinetly: if cancel becomes true while waiting and chefs went home, it will get stuck

				if (dish != null) {
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

	private void serve(Dish dish) {
		if (log.isTraceEnabled()) log.trace("Served {}", dish.getName());
		try {
			producer.sendTextMessage(dish.getId(), dish.getName());
		} catch (ProducerDeliveryException ex) {
			backupProducer.sendTextMessage(dish.getId(), dish.getName());
		}
	}

	private void handleIdle() {
		if ((System.currentTimeMillis() - timeOfLastDish) > TimeUnit.SECONDS.toMillis(consumerIdle)) {
			processBackedUpMessages();
		}
	}

	private void processBackedUpMessages() {
		if (log.isTraceEnabled()) log.trace("Processing backup...");
		try {
			backupProducer.resend(producer);
		} catch (BackupProcessException ex) {
			log.warn("Failed to resend backup: {}", ex.getMessage());
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
