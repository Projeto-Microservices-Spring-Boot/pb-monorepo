package com.edu.infnet.pb.store.kafka.event;

public record OrderCancelledEvent(
        Long orderId,
        String userExternalId,
        String reason
) {}
