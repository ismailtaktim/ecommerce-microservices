package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.event.PaymentRequestEvent;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    Payment processPayment(PaymentRequestEvent event);
    Payment refundPayment(UUID orderId, String reason);
    Payment getPaymentByOrderId(UUID orderId);
    List<Payment> getPaymentsByCustomerId(UUID customerId);
}