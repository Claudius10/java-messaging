package com.example.messaging.task.async;

import com.example.messaging.model.Dish;
import jakarta.jms.Destination;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class ServerTask implements Task {

	private final BlockingQueue<Dish> completedDishes;

	private final AtomicInteger counter = new AtomicInteger(0);

	private final Destination destination;

	private final JmsTemplate jmsTemplate;

	private final int pollTimeOut;

	private boolean cancel = false;

	@Override
	public void run() {
		log.info("Server started work");
		startWork();
	}

	private void startWork() {
		log.info("Starting work!");
		try {
			while (!Thread.currentThread().isInterrupted()) {

				if (cancel && completedDishes.isEmpty()) {
					log.info("Server shift ended");
					log.info("Served {} dishes", counter.get());
					break;
				}

				log.info("Dishes remaining {}", completedDishes.size());

				Dish dish = completedDishes.poll(pollTimeOut, TimeUnit.SECONDS); // can't wait indefinetly: if cancel becomes true while waiting and chefs went home, it will get stuck

				if (dish != null) {
					try {
						serve(dish);
					} catch (JmsException ex) {
						log.error("Customer does not like the dish", ex);
					}
				}
			}

		} catch (InterruptedException ex) {
			log.error("Unexpected Interruption", ex);
			Thread.currentThread().interrupt();
		}
	}

	private void serve(final Dish dish) throws JmsException {
		long id = dish.getId();
		log.info("Serving dish: {}", id);
		jmsTemplate.send(destination, session -> {
			TextMessage textMessage = session.createTextMessage("Delicious dish");
			textMessage.setLongProperty("id", dish.getId());
			return textMessage;
		});
		counter.incrementAndGet();
	}

	@Override
	public void cancel() {
		cancel = true;
	}
}
