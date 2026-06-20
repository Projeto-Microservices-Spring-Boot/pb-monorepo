package com.infnet.geolocalizacao.dto.copaEstadios;


import com.fasterxml.jackson.annotation.JsonProperty;


public record MatchDTO(
      String id,
     @JsonProperty("home_team_name_en")
     String timeA,
      @JsonProperty("away_team_name_en")
      String timeB,
      @JsonProperty("group")
      String grupo,
      @JsonProperty("local_date")
      String dataJogo,
      @JsonProperty("type")
      String tipo,
      @JsonProperty("stadium_id")
      String estadioId,

      String estadio
) {
}
