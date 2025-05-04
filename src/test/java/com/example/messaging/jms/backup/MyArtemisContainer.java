package com.example.messaging.jms.backup;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public class MyArtemisContainer extends GenericContainer<MyArtemisContainer> {

	public MyArtemisContainer() {
		super(DockerImageName.parse("apache/activemq-artemis:latest-alpine"));
		withEnv("ARTEMIS_USERNAME", "artemis");
		withEnv("ARTEMIS_PASSWORD", "artemis");
		setPortBindings(List.of("61616:61616", "8161:8161"));
	}
}