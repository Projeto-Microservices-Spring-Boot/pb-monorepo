package com.edu.infnet.pb.geolocalization.dto.figurinhas;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record TradeMatchResponseDTO(
        @JsonProperty("userIdA")
        UUID usuarioOrigemId,
        @JsonProperty("userIdB")
        UUID usuarioDestinoId,
        @JsonProperty("score")
        int score,
        @JsonProperty("userACanOffer")
        int quantidadeFigurinhasRepetidasUsuarioA ,
        @JsonProperty("userBCanOffer")
        int quantidadeFigurinhasRepetidasUsuarioB ,
        @JsonProperty("stickersUserAHasThatUserBNeeds")
        List<FigurinhaDTO> stickersQueOrigemOferece,
        @JsonProperty("stickersUserBHasThatUserANeeds")
        List<FigurinhaDTO> stickersQueDestinoOferece
) {
}
