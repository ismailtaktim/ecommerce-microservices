package com.ecommerce.order.entity;

public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAYMENT_COMPLETED,
    COMPLETED,
    CANCELLED,
    FAILED
}