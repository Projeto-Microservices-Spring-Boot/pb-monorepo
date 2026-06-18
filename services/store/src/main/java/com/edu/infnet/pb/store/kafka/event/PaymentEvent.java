package com.edu.infnet.pb.store.kafka.event;

import java.math.BigDecimal;

public record PaymentEvent(
        Long paymentId,
        Long orderId,
        String status,
        BigDecimal amount,
        String userExternalId
) {}
