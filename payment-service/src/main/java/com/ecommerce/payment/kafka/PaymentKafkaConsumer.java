package com.ecommerce.payment.kafka;

import com.ecommerce.payment.event.PaymentRequestEvent;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-request", groupId = "payment-service")
    public void handlePaymentRequest(String message) {
        try {
            log.info("Ödeme isteği alındı: {}", message);
            PaymentRequestEvent event = objectMapper.readValue(message, PaymentRequestEvent.class);
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Ödeme isteği işlenemedi: {}", e.getMessage(), e);
        }
    }
}