package org.gustalab.kafka_consumer_api;

import org.springframework.boot.SpringApplication;

public class TestKafkaConsumerApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(KafkaConsumerApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
