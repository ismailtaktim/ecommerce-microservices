package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.event.InventoryReleaseRequestEvent;
import com.ecommerce.inventory.event.InventoryReserveRequestEvent;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryKafkaConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-reserve-request", groupId = "inventory-service")
    public void handleReserveRequest(String message) {
        try {
            log.info("Stok rezervasyon isteği alındı: {}", message);
            InventoryReserveRequestEvent event = objectMapper.readValue(message, InventoryReserveRequestEvent.class);
            inventoryService.reserveInventoryForOrder(event);
        } catch (Exception e) {
            log.error("Stok rezervasyon isteği işlenemedi: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "inventory-release-request", groupId = "inventory-service")
    public void handleReleaseRequest(String message) {
        try {
            log.info("Stok serbest bırakma isteği alındı: {}", message);
            InventoryReleaseRequestEvent event = objectMapper.readValue(message, InventoryReleaseRequestEvent.class);
            inventoryService.releaseInventoryForOrder(event.getOrderId(), event.getReason());
        } catch (Exception e) {
            log.error("Stok serbest bırakma isteği işlenemedi: {}", e.getMessage(), e);
        }
    }
}