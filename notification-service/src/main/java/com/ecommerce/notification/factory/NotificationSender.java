package com.ecommerce.notification.factory;

import com.ecommerce.notification.entity.Notification;

public interface NotificationSender {
    boolean send(Notification notification);
    String getChannel();
}