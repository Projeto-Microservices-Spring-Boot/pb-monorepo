package com.infnet.geolocalizacao.dto.trocaEventos;


import com.infnet.geolocalizacao.model.Enums.PointType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateEventsDTO(
       @NotBlank
        String nome,
        String descricao,
      @NotNull
       String endereco,
       Double latitude,
       Double longitude,
        LocalDate dataInicio,
        LocalDate dataFim,
        PointType setType
) {
}
