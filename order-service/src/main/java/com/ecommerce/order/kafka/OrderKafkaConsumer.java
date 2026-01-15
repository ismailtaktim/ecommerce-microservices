package com.ecommerce.order.kafka;

import com.ecommerce.order.event.InventoryReservedEvent;
import com.ecommerce.order.event.PaymentCompletedEvent;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-reserved", groupId = "order-service")
    public void handleInventoryReserved(String message) {
        try {
            log.info("Inventory reserved event alındı: {}", message);
            InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);
            orderService.handleInventoryReserved(event.getOrderId(), event.isSuccess(), event.getFailureReason());
        } catch (Exception e) {
            log.error("Inventory reserved event işlenemedi: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-completed", groupId = "order-service")
    public void handlePaymentCompleted(String message) {
        try {
            log.info("Payment completed event alındı: {}", message);
            PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);
            orderService.handlePaymentCompleted(event.getOrderId(), event.isSuccess(), event.getFailureReason());
        } catch (Exception e) {
            log.error("Payment completed event işlenemedi: {}", e.getMessage());
        }
    }


}