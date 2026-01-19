package com.ecommerce.notification.kafka;

import com.ecommerce.notification.event.OrderEvent;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-created", groupId = "notification-service")
    public void handleOrderCreated(String message) {
        try {
            log.info("Order created event alındı: {}", message);
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            notificationService.sendOrderCreatedNotification(event);
        } catch (Exception e) {
            log.error("Order created event işlenemedi: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order-completed", groupId = "notification-service")
    public void handleOrderCompleted(String message) {
        try {
            log.info("Order completed event alındı: {}", message);
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            notificationService.sendOrderCompletedNotification(event);
        } catch (Exception e) {
            log.error("Order completed event işlenemedi: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "notification-service")
    public void handleOrderCancelled(String message) {
        try {
            log.info("Order cancelled event alındı: {}", message);
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            notificationService.sendOrderFailedNotification(event);
        } catch (Exception e) {
            log.error("Order cancelled event işlenemedi: {}", e.getMessage(), e);
        }
    }
}