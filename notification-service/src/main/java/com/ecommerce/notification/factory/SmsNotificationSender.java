package com.ecommerce.notification.factory;

import com.ecommerce.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsNotificationSender implements NotificationSender {

    @Override
    public boolean send(Notification notification) {
        log.info("═══════════════════════════════════════════");
        log.info("📱 SMS GÖNDERİLİYOR");
        log.info("Telefon: {}", notification.getRecipient());
        log.info("Mesaj: {}", notification.getContent());
        log.info("═══════════════════════════════════════════");

        // Simülasyon: %90 başarı
        boolean success = Math.random() < 0.90;
        log.info(success ? "✅ SMS gönderildi!" : "❌ SMS gönderilemedi!");
        return success;
    }

    @Override
    public String getChannel() {
        return "SMS";
    }
}