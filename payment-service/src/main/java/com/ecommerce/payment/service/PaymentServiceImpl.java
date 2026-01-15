package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentMethod;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.event.PaymentRequestEvent;
import com.ecommerce.payment.kafka.PaymentKafkaProducer;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentKafkaProducer kafkaProducer;
    private final Random random = new Random();

    @Override
    @Transactional
    public Payment processPayment(PaymentRequestEvent event) {
        log.info("Ödeme işleniyor: orderId={}, amount={}", event.getOrderId(), event.getAmount());

        // Zaten ödeme var mı kontrol et
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Bu sipariş için zaten ödeme mevcut: orderId={}", event.getOrderId());
            return paymentRepository.findByOrderId(event.getOrderId()).get();
        }

        // Payment oluştur
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getAmount())
                .currency(event.getCurrency() != null ? event.getCurrency() : "TRY")
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PROCESSING)
                .build();

        payment = paymentRepository.save(payment);

        // Ödeme simülasyonu
        boolean paymentSuccess = simulatePaymentProcessing();

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionRef(generateTransactionId());
            payment.setProcessedAt(LocalDateTime.now());
            log.info("Ödeme başarılı: orderId={}, transactionRef={}",
                    event.getOrderId(), payment.getTransactionRef());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("BANK_REJECTED");
            payment.setFailureMessage("Ödeme işlemi başarısız - Banka reddi");
            log.error("Ödeme başarısız: orderId={}", event.getOrderId());
        }

        payment = paymentRepository.save(payment);

        // Event gönder
        sendPaymentCompletedEvent(payment);

        return payment;
    }

    @Override
    @Transactional
    public Payment refundPayment(UUID orderId, String reason) {
        log.info("Ödeme iadesi: orderId={}, reason={}", orderId, reason);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı: " + orderId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Sadece tamamlanmış ödemeler iade edilebilir");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        // Not: refund işlemi ayrı refunds tablosunda yapılmalı

        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı: " + orderId));
    }

    @Override
    public List<Payment> getPaymentsByCustomerId(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    private boolean simulatePaymentProcessing() {
        // %90 başarı oranı simülasyonu
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return random.nextInt(100) < 90;
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    private void sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(payment.getOrderId())
                .paymentId(payment.getId())
                .success(payment.getStatus() == PaymentStatus.COMPLETED)
                .failureReason(payment.getFailureReason())
                .transactionId(payment.getTransactionRef())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaProducer.sendPaymentCompletedEvent(event);
    }
}