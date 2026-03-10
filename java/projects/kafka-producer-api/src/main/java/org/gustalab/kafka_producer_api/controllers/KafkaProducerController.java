package org.gustalab.kafka_producer_api.controllers;

import org.gustalab.kafka_producer_api.models.OrderDTO;
import org.gustalab.kafka_producer_api.services.KafkaProducerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/producer")
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerController {
    private final KafkaProducerService service;

    // @PostMapping("path")
    // public String postMethodName(@RequestBody String entity) {
    //     //TODO: process POST request
        
    //     return entity;
    // }

    @PostMapping("/orders")
    public ResponseEntity<Void> produceOrder(@RequestBody @Validated OrderDTO order){
        log.info(new StringBuilder().append("Iniciando criação de ordem: [").append(order).append("]").toString()); 
        try {
            ResponseEntity<Void> response = service.produceOrder(order);
            return ResponseEntity.status(response.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Erro ao produzir ordem: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}