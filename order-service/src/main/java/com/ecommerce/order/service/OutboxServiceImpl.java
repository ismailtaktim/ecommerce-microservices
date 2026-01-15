package com.ecommerce.order.service;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveEvent(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .published(false)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Outbox event kaydedildi: {} - {}", eventType, aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Event JSON'a çevrilemedi: {}", e.getMessage());
            throw new RuntimeException("Event serialization failed", e);
        }
    }

    @Override
    @Scheduled(fixedDelay = 5000) // Her 1 saniyede bir çalışır
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                // Kafka'ya gönder
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), event.getPayload());

                // Published olarak işaretle
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Event yayınlandı: {} - {}", event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Event yayınlanamadı: {} - {}", event.getEventType(), e.getMessage());
                // Hata durumunda döngüden çık, sonraki schedule'da tekrar denenecek
                break;
            }
        }
    }
}