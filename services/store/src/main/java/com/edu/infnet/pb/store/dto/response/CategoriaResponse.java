package com.edu.infnet.pb.store.dto.response;

import java.time.LocalDateTime;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        Boolean ativo,
        int quantidadeProdutos,
        LocalDateTime createdAt
) {}
