package com.ecommerce.notification.factory;

import com.ecommerce.notification.entity.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSenderFactory {

    private final List<NotificationSender> senders;
    private Map<String, NotificationSender> senderMap;

    public NotificationSender getSender(NotificationChannel channel) {
        if (senderMap == null) {
            senderMap = senders.stream()
                    .collect(Collectors.toMap(
                            NotificationSender::getChannel,
                            Function.identity()
                    ));
        }

        NotificationSender sender = senderMap.get(channel.name());

        if (sender == null) {
            throw new IllegalArgumentException("Bilinmeyen kanal: " + channel);
        }

        return sender;
    }
}