package org.gustalab.kafka_consumer_api.order.model;

import java.io.Serializable;

public record OrderDTO (
    String orderId,
    String customerId,
    Long amount
) implements Serializable {}
