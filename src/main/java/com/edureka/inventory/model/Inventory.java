package com.edureka.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Inventory item - tracks stock quantity per product (by skuCode).
 * Each service has its own database per microservices pattern.
 */
@Document(collection = "inventory")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Inventory {
    @Id
    private String id;
    private String skuCode;
    private Integer quantity;
}
