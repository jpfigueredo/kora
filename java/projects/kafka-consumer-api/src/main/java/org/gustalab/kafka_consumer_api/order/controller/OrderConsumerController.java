package org.gustalab.kafka_consumer_api.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/consumer")
@Slf4j
public class OrderConsumerController {

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        log.info("Health check do consumer chamado");
        return ResponseEntity.ok().build();
    }
}