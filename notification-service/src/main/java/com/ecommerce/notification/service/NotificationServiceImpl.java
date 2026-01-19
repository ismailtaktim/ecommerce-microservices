package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.entity.NotificationChannel;
import com.ecommerce.notification.entity.NotificationStatus;
import com.ecommerce.notification.entity.NotificationType;
import com.ecommerce.notification.event.OrderEvent;
import com.ecommerce.notification.factory.NotificationSender;
import com.ecommerce.notification.factory.NotificationSenderFactory;
import com.ecommerce.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSenderFactory senderFactory;

    @Override
    @Transactional
    public Notification sendNotification(OrderEvent event, NotificationType type,
                                         NotificationChannel channel, String subject, String content) {

        String recipient = channel == NotificationChannel.SMS
                ? event.getCustomerPhone()
                : event.getCustomerEmail();

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .recipient(recipient)
                .type(type)
                .channel(channel)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        // Factory'den doğru sender'ı al
        NotificationSender sender = senderFactory.getSender(channel);

        // Gönder
        boolean success = sender.send(notification);

        if (success) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedReason("Gönderim başarısız");
        }

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendOrderCreatedNotification(OrderEvent event) {
        log.info("📧 Sipariş oluşturuldu bildirimi: {}", event.getOrderNumber());

        String subject = "Siparişiniz Alındı - " + event.getOrderNumber();
        String emailContent = String.format(
                "Sayın Müşterimiz,\n\nSiparişiniz alındı.\nSipariş No: %s\nTutar: %.2f TL\n\nTeşekkürler!",
                event.getOrderNumber(), event.getTotalAmount()
        );
        String smsContent = String.format("Siparişiniz alındı. No: %s, Tutar: %.2f TL",
                event.getOrderNumber(), event.getTotalAmount());

        sendNotification(event, NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, subject, emailContent);
        sendNotification(event, NotificationType.ORDER_CREATED, NotificationChannel.SMS, subject, smsContent);
    }

    @Override
    @Transactional
    public void sendOrderCompletedNotification(OrderEvent event) {
        log.info("📧 Sipariş tamamlandı bildirimi: {}", event.getOrderNumber());

        String subject = "Siparişiniz Onaylandı - " + event.getOrderNumber();
        String emailContent = String.format(
                "Sayın Müşterimiz,\n\nSiparişiniz onaylandı.\nSipariş No: %s\nTutar: %.2f TL\n\nTeşekkürler!",
                event.getOrderNumber(), event.getTotalAmount()
        );
        String smsContent = String.format("Siparişiniz onaylandı. No: %s", event.getOrderNumber());

        sendNotification(event, NotificationType.ORDER_COMPLETED, NotificationChannel.EMAIL, subject, emailContent);
        sendNotification(event, NotificationType.ORDER_COMPLETED, NotificationChannel.SMS, subject, smsContent);
    }

    @Override
    @Transactional
    public void sendOrderFailedNotification(OrderEvent event) {
        log.info("📧 Sipariş iptal bildirimi: {}", event.getOrderNumber());

        String subject = "Siparişiniz İptal Edildi - " + event.getOrderNumber();
        String emailContent = String.format(
                "Sayın Müşterimiz,\n\nSiparişiniz iptal edildi.\nSipariş No: %s\nSebep: %s\n\nÖzür dileriz!",
                event.getOrderNumber(), event.getReason()
        );
        String smsContent = String.format("Siparişiniz iptal edildi. No: %s", event.getOrderNumber());

        sendNotification(event, NotificationType.ORDER_CANCELLED, NotificationChannel.EMAIL, subject, emailContent);
        sendNotification(event, NotificationType.ORDER_CANCELLED, NotificationChannel.SMS, subject, smsContent);
    }

    @Override
    public List<Notification> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findByOrderId(orderId);
    }
}