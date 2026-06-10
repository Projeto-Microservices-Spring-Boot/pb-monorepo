package com.edu.infnet.pb.payments.dto;

import com.edu.infnet.pb.payments.enums.PaymentMethod;
import com.edu.infnet.pb.payments.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID userId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
