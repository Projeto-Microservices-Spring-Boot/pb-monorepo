package com.edu.infnet.pb.store.dto.event;

import java.math.BigDecimal;

public record PedidoCriadoEvent(
        Long orderId,
        Long userId,
        BigDecimal amount
) {
}
