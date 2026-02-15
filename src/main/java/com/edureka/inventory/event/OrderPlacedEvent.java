package com.edureka.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Event consumed from Kafka when order is placed.
 * Must match structure from Order service for deserialization.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private String orderNumber;
    private String orderId;
    private String email;
    private String skuCode;
    private Integer quantity;
    private BigDecimal amount;
}
