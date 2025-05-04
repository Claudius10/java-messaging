package com.example.messaging.kafka.backup;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public class MyKafkaContainer extends GenericContainer<MyKafkaContainer> {

	public MyKafkaContainer() {
		super(DockerImageName.parse("apache/kafka:4.0.0"));
		setPortBindings(List.of("9092:9092"));
	}
}
