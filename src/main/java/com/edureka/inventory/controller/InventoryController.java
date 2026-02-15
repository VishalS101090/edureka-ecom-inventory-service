package com.edureka.inventory.controller;

import com.edureka.inventory.model.Inventory;
import com.edureka.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Inventory Controller - manages stock levels.
 * Used by Order service (sync) to check availability before placing order.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    public static final Logger _logger = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private InventoryRepository repository;

    /**
     * Check if sufficient stock is available for a given skuCode and quantity.
     * Used by Order service for inter-service communication.
     * Returns 200 with { "inStock": true/false } or 404 if product not in inventory.
     */
    @GetMapping("/check")
    public ResponseEntity<?> isInStock(
            @RequestParam String skuCode,
            @RequestParam Integer quantity) {
        if (skuCode == null || skuCode.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "skuCode is required"));
        }
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "quantity must be positive"));
        }
        _logger.info("Checking stock for skuCode: {}, quantity: {}", skuCode, quantity);
        Optional<Inventory> inv = repository.findBySkuCode(skuCode);
        if (inv.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Inventory not found for skuCode: " + skuCode,
                            "inStock", false));
        }
        boolean inStock = inv.get().getQuantity() >= quantity;
        return ResponseEntity.ok(Map.of("inStock", inStock, "availableQuantity", inv.get().getQuantity()));
    }

    /**
     * Get all inventory items.
     */
    @GetMapping(value = {"", "/", "/all"})
    public ResponseEntity<?> getAllInventory() {
        _logger.info("Getting all inventory");
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * Get inventory by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getInventoryById(@PathVariable String id) {
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Inventory id is required"));
        }
        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Inventory not found for id: " + id)));
    }

    /**
     * Get inventory by skuCode.
     */
    @GetMapping("/sku/{skuCode}")
    public ResponseEntity<?> getInventoryBySkuCode(@PathVariable String skuCode) {
        if (skuCode == null || skuCode.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "skuCode is required"));
        }
        return repository.findBySkuCode(skuCode)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Inventory not found for skuCode: " + skuCode)));
    }

    /**
     * Add or update inventory item.
     */
    @PostMapping(value = {"", "/"})
    public ResponseEntity<?> createOrUpdateInventory(@RequestBody Inventory inventory) {
        if (inventory == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Inventory body is required"));
        }
        if (inventory.getSkuCode() == null || inventory.getSkuCode().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "skuCode is required"));
        }
        if (inventory.getQuantity() != null && inventory.getQuantity() < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Quantity must be non-negative"));
        }

        Optional<Inventory> existing = repository.findBySkuCode(inventory.getSkuCode());
        if (existing.isPresent()) {
            Inventory toUpdate = existing.get();
            if (inventory.getQuantity() != null) toUpdate.setQuantity(inventory.getQuantity());
            Inventory updated = repository.save(toUpdate);
            _logger.info("Inventory updated for skuCode: {}", inventory.getSkuCode());
            return ResponseEntity.ok(updated);
        } else {
            inventory.setId(inventory.getId() != null && !inventory.getId().isBlank()
                    ? inventory.getId() : UUID.randomUUID().toString());
            if (inventory.getQuantity() == null) inventory.setQuantity(0);
            Inventory saved = repository.save(inventory);
            _logger.info("New inventory added for skuCode: {}", saved.getSkuCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }
    }

    /**
     * Deduct quantity from stock (used internally by listener or by API).
     */
    @PutMapping("/deduct/{skuCode}")
    public ResponseEntity<?> deductStock(@PathVariable String skuCode, @RequestParam Integer quantity) {
        if (skuCode == null || skuCode.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "skuCode is required"));
        }
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "quantity must be positive"));
        }
        Optional<Inventory> opt = repository.findBySkuCode(skuCode);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Inventory not found for skuCode: " + skuCode));
        }
        Inventory inv = opt.get();
        if (inv.getQuantity() < quantity) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Insufficient stock. Available: " + inv.getQuantity()));
        }
        inv.setQuantity(inv.getQuantity() - quantity);
        Inventory updated = repository.save(inv);
        _logger.info("Deducted {} from stock for skuCode: {}, remaining: {}", quantity, skuCode, updated.getQuantity());
        return ResponseEntity.ok(updated);
    }
}
