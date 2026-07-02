package com.edu.infnet.pb.store.kafka.event;

public record OrderStatusChangedEvent(
        Long orderId,
        String userExternalId,
        String previousStatus,
        String newStatus
) {}
