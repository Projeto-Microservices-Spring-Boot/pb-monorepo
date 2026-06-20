package com.infnet.geolocalizacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeoLocalizacaoDosEtadiosDTO(
        @JsonProperty("estadio")
        String nome ,
        Double latitude,
        Double longitude
) {
}
