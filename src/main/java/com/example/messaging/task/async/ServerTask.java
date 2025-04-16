package com.example.messaging.task.async;

import com.example.messaging.util.JmsConnection;
import jakarta.jms.JMSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class ServerTask implements Task {

	private final CountDownLatch startGate;

	private final BlockingQueue<Long> completedDishes;

	private final AtomicInteger in = new AtomicInteger(0);

	private final AtomicInteger out = new AtomicInteger(0);

	private final JmsConnection jmsConnection;

//	private final JmsTemplate jmsTemplate;

	private final int pollTimeOut;

	private boolean cancel = false;

	@Override
	public void run() {
		startWork();
	}

	private void startWork() {
		try {
			log.info("Waiting on coworkers...");
			startGate.await();
			log.info("All coworkers ready, starting work!");

			while (!Thread.currentThread().isInterrupted()) {

				if (cancel && completedDishes.isEmpty()) {
					log.info("Server shift ended");
					break;
				}

				Long dish = completedDishes.poll(pollTimeOut, TimeUnit.MILLISECONDS); // can't wait indefinetly: if cancel becomes true while waiting and chefs went home, it will get stuck

				if (dish != null) {
					try {
						in.incrementAndGet();
						serve(dish);
						out.incrementAndGet();
					} catch (JMSException ex) {
						log.error("Customer does not like the dish", ex);
					}
				} else {
					// wait
				}
			}
		} catch (InterruptedException ex) {
			log.error("Unexpected Interruption", ex);
			Thread.currentThread().interrupt();
		}
	}

	private void serve(long id) throws JMSException {
		jmsConnection.send(id);
//		jmsTemplate.send(session -> {
//			TextMessage textMessage = session.createTextMessage("Delicious Dish");
//			textMessage.setLongProperty("id", id);
//			return textMessage;
//		});
	}

	@Override
	public void cancel() {
		cancel = true;
	}

	@Override
	public int getInCount() {
		return in.get();
	}

	@Override
	public int getOutCount() {
		return out.get();
	}
}
