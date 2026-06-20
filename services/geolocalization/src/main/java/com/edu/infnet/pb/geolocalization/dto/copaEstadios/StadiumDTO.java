package com.infnet.geolocalizacao.dto.copaEstadios;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StadiumDTO(
        String id ,

        @JsonProperty("name_en")
        String nome,

        @JsonProperty("city_en")
        String cidade,

        @JsonProperty("coutry_en")
        String pais

) {
}
