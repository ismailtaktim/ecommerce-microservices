package com.ecommerce.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private UUID orderId;
    private UUID paymentId;
    private boolean success;
    private String failureReason;
    private String transactionId;
    private LocalDateTime timestamp;
}