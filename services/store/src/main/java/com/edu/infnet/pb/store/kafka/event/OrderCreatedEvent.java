package com.edu.infnet.pb.store.kafka.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        String userExternalId,
        BigDecimal totalValue,
        int itemCount
) {}
