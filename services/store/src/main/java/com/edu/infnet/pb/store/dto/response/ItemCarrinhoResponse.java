package com.edu.infnet.pb.store.dto.response;

import java.math.BigDecimal;

public record ItemCarrinhoResponse(
        Long itemId,
        Long produtoId,
        String nomeProduto,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}
