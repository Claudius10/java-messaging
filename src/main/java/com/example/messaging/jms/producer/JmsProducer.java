package com.example.messaging.jms.producer;

import com.example.messaging.common.producer.Producer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;

public interface JmsProducer extends Producer {

	JMSContext getContext();

	JMSProducer getProducer();
}
