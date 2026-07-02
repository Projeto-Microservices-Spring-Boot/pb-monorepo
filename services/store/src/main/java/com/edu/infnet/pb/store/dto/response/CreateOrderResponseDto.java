package com.edu.infnet.pb.store.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderResponseDto(
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String status
) {
}