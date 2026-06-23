package com.edu.infnet.pb.geolocalization.dto.figurinhas;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FigurinhaDTO(
        @JsonProperty("stickerId")
        Long figurinhaId ,
        @JsonProperty("playerName")
        String nomeJogador
) {
}
