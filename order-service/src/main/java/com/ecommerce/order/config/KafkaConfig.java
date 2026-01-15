package com.ecommerce.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KafkaConfig {

    // Order Service'in yayınladığı topic'ler
    @Bean
    public NewTopic inventoryReserveRequestTopic() {
        return TopicBuilder.name("inventory-reserve-request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleaseRequestTopic() {
        return TopicBuilder.name("inventory-release-request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentRequestTopic() {
        return TopicBuilder.name("payment-request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentRefundRequestTopic() {
        return TopicBuilder.name("payment-refund-request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order-created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name("order-completed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order-cancelled")
                .partitions(3)
                .replicas(1)
                .build();
    }
}