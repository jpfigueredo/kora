package org.gustalab.kafka_producer_api.services;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.gustalab.kafka_producer_api.models.OrderDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KafkaProducerService {

    private final KafkaProducer<String, String> producer;

    public KafkaProducerService() {
        Properties props = new Properties();
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);
        this.producer = new KafkaProducer<>(props);
    }

    public ResponseEntity<Void> produceOrder(OrderDTO order) {
        log.info("Iniciando produção de ordem: [{}]", order);
        String payload = String.format(
            "{\"orderId\":\"%s\",\"customerId\":\"%s\",\"amount\":%d}",
            order.orderId(),
            order.customerId(),
            order.amount()
        );
        producer.send(new ProducerRecord<>("order-topic", payload));
        producer.flush();
        log.info("Produção de ordem finalizada");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}