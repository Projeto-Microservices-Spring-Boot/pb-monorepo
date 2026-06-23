package com.edu.infnet.pb.geolocalization.dto.kafka;

import com.edu.infnet.pb.geolocalization.event.StickerOfertaDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PropostaAceitaEvento(
        Long propostaId,
        UUID usuarioOrigem,
        UUID usuarioDestino,
        List<StickerOfertaDTO> stickerOferecidoOrigem,
        List<StickerOfertaDTO> stickerOferecidoDestino,
        LocalDateTime dataAceite
) {
}