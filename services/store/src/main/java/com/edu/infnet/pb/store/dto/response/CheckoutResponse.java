package com.edu.infnet.pb.store.dto.response;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long pedidoId,
        Long userId,
        BigDecimal valorTotal,
        String status,
        String mensagem
) {
}
