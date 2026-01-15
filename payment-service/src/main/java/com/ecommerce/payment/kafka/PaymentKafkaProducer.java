package com.ecommerce.payment.kafka;

import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment-completed";

    public void sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, event.getOrderId().toString(), payload);
            log.info("Payment completed event gönderildi: orderId={}, success={}",
                    event.getOrderId(), event.isSuccess());
        } catch (JsonProcessingException e) {
            log.error("Event JSON'a çevrilemedi: {}", e.getMessage());
        }
    }
}