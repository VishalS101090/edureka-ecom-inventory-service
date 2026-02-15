package com.edureka.inventory.listener;

import com.edureka.inventory.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InventoryListener {

    @KafkaListener(topics = "notificationTopic", groupId = "inventory-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        // Logic: Update stock in database
        log.info("Received Notification for Order - " + event.getOrderNumber());
        System.out.println("Processing inventory for order: " + event.getOrderNumber());
    }
}