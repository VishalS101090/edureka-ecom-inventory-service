package com.edureka.inventory.listener;

import com.edureka.inventory.event.OrderPlacedEvent;
import com.edureka.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InventoryListener {

    @Autowired
    private InventoryRepository repository;

    @KafkaListener(topics = "orderPlacedTopic", groupId = "inventory-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Processing inventory for order: {}", event.getOrderNumber());

        if (event.getSkuCode() == null || event.getSkuCode().isBlank()) {
            log.warn("Order {} has no skuCode, skipping inventory update", event.getOrderNumber());
            return;
        }
        Integer quantity = event.getQuantity() != null ? event.getQuantity() : 1;

        repository.findBySkuCode(event.getSkuCode()).ifPresentOrElse(
                inv -> {
                    if (inv.getQuantity() >= quantity) {
                        inv.setQuantity(inv.getQuantity() - quantity);
                        repository.save(inv);
                        log.info("Deducted {} from stock for skuCode: {}, remaining: {}",
                                quantity, event.getSkuCode(), inv.getQuantity());
                    } else {
                        log.warn("Insufficient stock for order {}. skuCode: {}, required: {}, available: {}",
                                event.getOrderNumber(), event.getSkuCode(), quantity, inv.getQuantity());
                    }
                },
                () -> log.warn("Inventory not found for skuCode: {}, order: {}",
                        event.getSkuCode(), event.getOrderNumber())
        );
    }
}