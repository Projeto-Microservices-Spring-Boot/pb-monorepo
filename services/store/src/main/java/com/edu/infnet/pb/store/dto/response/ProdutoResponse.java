package com.edu.infnet.pb.store.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoque,
        String imagemUrl,
        Boolean ativo,
        Long categoriaId,
        String categoriaNome,
        LocalDateTime createdAt
) {}
