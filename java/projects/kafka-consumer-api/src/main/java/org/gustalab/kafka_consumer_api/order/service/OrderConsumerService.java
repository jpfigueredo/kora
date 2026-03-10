package org.gustalab.kafka_consumer_api.order.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.gustalab.kafka_consumer_api.order.model.OrderDTO;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderConsumerService {
    
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderConsumerService() {
        Properties props = new Properties();
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.consumer = new KafkaConsumer<>(props);
    }

    @PostConstruct
    public void start() {
        Thread t = new Thread(this::runConsumer, "order-consumer-thread");
        t.setDaemon(true);
        t.start();
    }

    private void runConsumer() {
        log.info("Iniciando consumo de ordens");
        consumer.subscribe(Collections.singletonList("order-topic"));
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                log.info("Mensagem recebida: {}", record.value());
                try {
                    OrderDTO order = objectMapper.readValue(record.value(), OrderDTO.class);
                    log.info("Processando ordem: {}", order);
                    // processamento de negócio iria aqui
                    log.info("Ordem processada com sucesso");
                } catch (Exception e) {
                    log.error("Erro ao desserializar/processar mensagem", e);
                }
            }
        }
    }
}