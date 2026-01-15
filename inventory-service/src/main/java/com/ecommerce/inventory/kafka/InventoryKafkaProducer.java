package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.event.InventoryReservedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_INVENTORY_RESERVED = "inventory-reserved";

    public void sendInventoryReservedEvent(InventoryReservedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_INVENTORY_RESERVED, event.getOrderId().toString(), payload);
            log.info("Inventory reserved event gönderildi: orderId={}, success={}",
                    event.getOrderId(), event.isSuccess());
        } catch (JsonProcessingException e) {
            log.error("Event JSON'a çevrilemedi: {}", e.getMessage());
        }
    }
}