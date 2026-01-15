package com.ecommerce.order.service;

import java.util.UUID;

public interface OutboxService {

    void saveEvent(String aggregateType, UUID aggregateId, String eventType, Object payload);

    void publishPendingEvents();
}