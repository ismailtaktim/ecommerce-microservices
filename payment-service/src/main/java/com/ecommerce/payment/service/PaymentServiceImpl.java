package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentMethod;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.event.PaymentRequestEvent;
import com.ecommerce.payment.exception.PaymentDeclinedException;
import com.ecommerce.payment.kafka.PaymentKafkaProducer;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentKafkaProducer kafkaProducer;
    private final BankService bankService;

    private static final int MAX_RETRY_COUNT = 5;

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

        // Banka çağrısı
        return processBankPayment(payment);
    }

    private Payment processBankPayment(Payment payment) {
        try {
            BankResponse response = bankService.charge("4111111111111111", payment.getAmount());

            if (response.isSuccess()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionRef(response.getTransactionId());
                payment.setProcessedAt(LocalDateTime.now());
                log.info("Ödeme başarılı: orderId={}, transactionRef={}",
                        payment.getOrderId(), payment.getTransactionRef());
            } else if (response.isRetryLater()) {
                // Banka geçici hatası - kuyruğa al
                payment.setStatus(PaymentStatus.PENDING);
                payment.setRetryCount(payment.getRetryCount() + 1);
                payment.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
                payment.setLastError(response.getMessage());
                log.warn("Ödeme kuyruğa alındı: orderId={}, nextRetry={}",
                        payment.getOrderId(), payment.getNextRetryAt());

                // Event gönderme - beklemede
                payment = paymentRepository.save(payment);
                return payment; // Event göndermiyoruz, retry edilecek
            } else {
                // Kart reddedildi
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(response.getErrorCode());
                payment.setFailureMessage(response.getMessage());
                log.error("Ödeme reddedildi: orderId={}, reason={}",
                        payment.getOrderId(), response.getErrorCode());
            }

        } catch (PaymentDeclinedException e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getDeclineCode());
            payment.setFailureMessage(e.getMessage());
            log.error("Ödeme reddedildi: orderId={}", payment.getOrderId());
        } catch (Exception e) {
            // Beklenmeyen hata - kuyruğa al
            payment.setStatus(PaymentStatus.PENDING);
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
            payment.setLastError(e.getMessage());
            log.error("Beklenmeyen hata, kuyruğa alındı: {}", e.getMessage());

            payment = paymentRepository.save(payment);
            return payment;
        }

        payment = paymentRepository.save(payment);
        sendPaymentCompletedEvent(payment);
        return payment;
    }

    @Scheduled(fixedDelay = 60000) // Her 1 dakikada bir
    @Transactional
    public void retryPendingPayments() {
        List<Payment> pendingPayments = paymentRepository
                .findByStatusAndNextRetryAtBefore(PaymentStatus.PENDING, LocalDateTime.now());

        for (Payment payment : pendingPayments) {
            if (payment.getRetryCount() >= MAX_RETRY_COUNT) {
                // Maksimum deneme aşıldı
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("MAX_RETRY_EXCEEDED");
                payment.setFailureMessage("Maksimum deneme sayısı aşıldı");
                paymentRepository.save(payment);
                sendPaymentCompletedEvent(payment);
                log.error("Ödeme başarısız - max retry: orderId={}", payment.getOrderId());
            } else {
                log.info("Ödeme retry ediliyor: orderId={}, attempt={}",
                        payment.getOrderId(), payment.getRetryCount() + 1);
                processBankPayment(payment);
            }
        }
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