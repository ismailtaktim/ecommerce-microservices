package com.ecommerce.notification.factory;

import com.ecommerce.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        log.info("═══════════════════════════════════════════");
        log.info("📧 EMAIL GÖNDERİLİYOR");
        log.info("Alıcı: {}", notification.getRecipient());
        log.info("Konu: {}", notification.getSubject());
        log.info("İçerik: {}", notification.getContent());
        log.info("═══════════════════════════════════════════");

        // Simülasyon: %95 başarı
        boolean success = Math.random() < 0.95;
        log.info(success ? "✅ Email gönderildi!" : "❌ Email gönderilemedi!");
        return success;
    }

    @Override
    public String getChannel() {
        return "EMAIL";
    }
}