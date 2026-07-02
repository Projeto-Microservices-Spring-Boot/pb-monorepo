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
}
