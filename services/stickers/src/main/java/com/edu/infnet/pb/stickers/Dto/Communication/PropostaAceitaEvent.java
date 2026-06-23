package com.edu.infnet.pb.stickers.Dto.Communication;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PropostaAceitaEvent {
    private Long propostaId;
    private UUID usuarioOrigem;
    private UUID usuarioDestino;
    private Long stickerOferecidoOrigem;
    private Integer quantidadeOferecidaOrigem;
    private Long stickerOferecidoDestino;
    private Integer quantidadeOferecidaDestino;

}

