package com.edu.infnet.pb.store.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CarrinhoResponse(
        Long id,
        String usuarioExternalId,
        List<ItemCarrinhoResponse> itens,
        BigDecimal total,
        int quantidadeItens
) {
    public record ItemCarrinhoResponse(
            Long id,
            Long produtoId,
            String produtoNome,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {}
}
