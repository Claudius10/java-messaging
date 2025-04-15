package com.example.messaging.task.async;

import com.example.messaging.model.Dish;
import com.example.messaging.model.Dishes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class ChefTask implements Task {

	private final BlockingQueue<Dish> completedDishes;

	private final AtomicInteger counter = new AtomicInteger(0);

	private final Dishes dishes;

	private boolean cancel = false;

	@Override
	public void run() {
		log.info("Chef started work");
		startWork();
	}

	private void startWork() {
		try {
			while (!Thread.currentThread().isInterrupted()) {

				if (cancel) {
					log.info("Chef shift ended");
					log.info("Chef cooked {} dishes", counter.get());
					break;
				}

				Dish dish = dishes.get();

				if (dish != null) {
					cook(dish);
					completedDishes.put(dish); // wait: can't have customers go hungry. also, if cancel becomes true, put last dish and end shift
				}
			}
		} catch (InterruptedException ex) {
			log.error("Unexpected Interruption", ex);
			Thread.currentThread().interrupt();
		}
	}

	private void cook(Dish dish) {
		dish.setCooked(true);
		log.info("Chef cooked dish {}", dish.getId());
		counter.incrementAndGet();
	}

	@Override
	public void cancel() {
		cancel = true;
	}
}
