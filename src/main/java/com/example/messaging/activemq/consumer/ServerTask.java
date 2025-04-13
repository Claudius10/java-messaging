package com.example.messaging.activemq.consumer;

import com.example.messaging.model.Dish;
import jakarta.jms.Destination;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class ServerTask implements Runnable {

	boolean working = false;

	private final BlockingQueue<Dish> completedDishes;

	private final Destination destination;

	private final JmsTemplate jmsTemplate;

	private final int delay;

	@Override
	public void run() {
		working = true;
		log.info("ServerTask started work");
		serve();
	}

	private void serve() {
		while (working) {
			try {
				Dish dish = completedDishes.poll(delay, TimeUnit.SECONDS);

				if (dish == null) {
					stopWork();
				} else {
					try {
						serve(dish);
					} catch (JmsException ex) {
						log.error("Customer does not like the dish", ex);
					}
				}
			} catch (InterruptedException ex) {
				log.error("ServerTask interrupted {}", ex.getMessage());
				Thread.currentThread().interrupt();
			}
		}
	}


	private void serve(final Dish dish) throws JmsException {
		long id = dish.getId();
		log.info("Serving dish: {}", id);
		jmsTemplate.send(destination, session -> {
			TextMessage textMessage = session.createTextMessage("Delicious");
			textMessage.setLongProperty("id", dish.getId());
			return textMessage;
		});
	}

	public void stopWork() {
		working = false;
		log.info("ServerTask is resting");
	}
}
