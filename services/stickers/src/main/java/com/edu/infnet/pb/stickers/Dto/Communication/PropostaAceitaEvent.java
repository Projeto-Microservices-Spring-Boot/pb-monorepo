package com.edu.infnet.pb.stickers.Dto.Communication;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PropostaAceitaEvent {
    private Long propostaId;
    private UUID usuarioOrigem;
    private UUID usuarioDestino;
    private List<StickerOfertDTO> stickerOferecidoOrigem;
    private List<StickerOfertDTO> stickerOferecidoDestino;
    private LocalDateTime dataAceite;

}