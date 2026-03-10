package org.gustalab.kafka_producer_api.models;

import java.io.Serializable;

public record OrderDTO (
    String orderId,
    String customerId,
    Long amount
) implements Serializable {}
