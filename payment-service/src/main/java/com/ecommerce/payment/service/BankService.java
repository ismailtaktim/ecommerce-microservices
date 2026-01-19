package com.ecommerce.payment.service;

import com.ecommerce.payment.exception.BankConnectionException;
import com.ecommerce.payment.exception.PaymentDeclinedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Service
@Slf4j
public class BankService {

    private final Random random = new Random();
    private int callCount = 0;

    @Retry(name = "bankService", fallbackMethod = "fallbackCharge")
    @CircuitBreaker(name = "bankService", fallbackMethod = "fallbackCharge")
    public BankResponse charge(String cardNumber, BigDecimal amount) {
        callCount++;
        log.info("Banka çağrısı #{}: {} TL", callCount, amount);

        // Simülasyon: Farklı senaryolar
        int scenario = random.nextInt(100);

        // %10 - Banka bağlantı hatası (retry edilecek)
        if (scenario < 10) {
            log.warn("Banka bağlantı hatası!");
            throw new BankConnectionException("Banka sunucusuna bağlanılamadı");
        }

        // %10 - Timeout (retry edilecek)
        if (scenario < 20) {
            log.warn("Banka timeout!");
            try {
                Thread.sleep(15000); // 15 saniye bekle (timeout olacak)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new BankConnectionException("Banka yanıt vermedi - timeout");
        }

        // %10 - Kart reddedildi (retry edilmeyecek)
        if (scenario < 30) {
            log.warn("Kart reddedildi!");
            throw new PaymentDeclinedException("Kart reddedildi - yetersiz bakiye", "INSUFFICIENT_FUNDS");
        }

        // %70 - Başarılı
        log.info("Ödeme başarılı!");
        return BankResponse.builder()
                .success(true)
                .transactionId("TXN-" + System.currentTimeMillis())
                .message("Ödeme onaylandı")
                .build();
    }

    public BankResponse fallbackCharge(String cardNumber, BigDecimal amount, Exception e) {
        log.error("Fallback çağrıldı. Hata: {}", e.getMessage());

        if (e instanceof PaymentDeclinedException) {
            // Kart reddedildi - retry anlamsız
            return BankResponse.builder()
                    .success(false)
                    .errorCode(((PaymentDeclinedException) e).getDeclineCode())
                    .message(e.getMessage())
                    .build();
        }

        // Bağlantı hatası - sonra tekrar denenecek
        return BankResponse.builder()
                .success(false)
                .errorCode("BANK_UNAVAILABLE")
                .message("Banka şu anda hizmet veremiyor, ödeme kuyruğa alındı")
                .retryLater(true)
                .build();
    }
}