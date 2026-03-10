package org.gustalab.kafka_producer_api;

import org.springframework.boot.SpringApplication;

public class TestKafkaProducerApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(KafkaProducerApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
