package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.entity.NotificationChannel;
import com.ecommerce.notification.entity.NotificationType;
import com.ecommerce.notification.event.OrderEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification sendNotification(OrderEvent event, NotificationType type, NotificationChannel channel, String subject, String content);
    void sendOrderCreatedNotification(OrderEvent event);
    void sendOrderCompletedNotification(OrderEvent event);
    void sendOrderFailedNotification(OrderEvent event);
    List<Notification> getNotificationsByOrderId(UUID orderId);
}