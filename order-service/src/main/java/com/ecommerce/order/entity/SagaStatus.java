package com.ecommerce.order.entity;

public enum SagaStatus {
    STARTED,
    INVENTORY_PENDING,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    NOTIFICATION_PENDING,
    COMPLETED,
    COMPENSATING,
    FAILED
}