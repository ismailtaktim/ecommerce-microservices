package com.ecommerce.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal totalAmount;
    private String reason;
    private LocalDateTime timestamp;
}