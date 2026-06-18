package com.edu.infnet.pb.store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String usuarioExternalId,
        String status,
        BigDecimal valorTotal,
        List<ItemPedidoResponse> itens,
        LocalDateTime createdAt
) {
    public record ItemPedidoResponse(
            Long id,
            Long produtoId,
            String produtoNome,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {}
}
