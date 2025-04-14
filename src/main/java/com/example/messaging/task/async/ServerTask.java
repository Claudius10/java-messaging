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

	private final int delay;

	private boolean working = true;

	private boolean cancel = false;

	@Override
	public Boolean call() {
		log.info("Server started work");
		return serve();
	}

	private Boolean serve() {
		try {
			while (working) {

				if (cancel && completedDishes.isEmpty()) {
					stop();
					return working;
				}

				Dish dish = completedDishes.poll(delay, TimeUnit.SECONDS);

				log.info("Dishes remaining {}", completedDishes.size());

				if (dish == null) {
					//
				} else {
					try {
						serve(dish);
					} catch (JmsException ex) {
						log.error("Customer does not like the get", ex);
					}
				}
			}

		} catch (InterruptedException ex) {
			log.error("Server interrupted {}", ex.getMessage());
			Thread.currentThread().interrupt();
		}

		return working;
	}

	private void serve(final Dish dish) throws JmsException {
		long id = dish.getId();
		log.info("Serving get: {}", id);
		jmsTemplate.send(destination, session -> {
			TextMessage textMessage = session.createTextMessage("Delicious get");
			textMessage.setLongProperty("id", dish.getId());
			return textMessage;
		});
		counter.incrementAndGet();
	}

	@Override
	public void cancel() {
		cancel = true;
		log.info("Server work cancelled");
	}

	private void stop() {
		working = false;
		log.info("Server shift ended");
		log.info("Served {} dishes", counter.get());
	}
}
