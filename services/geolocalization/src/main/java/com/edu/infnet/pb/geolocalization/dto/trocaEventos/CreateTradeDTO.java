package com.infnet.geolocalizacao.dto.trocaEventos;


import com.infnet.geolocalizacao.model.Enums.PointType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTradeDTO(
        @NotBlank
        String nome,
        String descricao,
        @NotNull
        String endereco,
        Double latitude,
        Double longitude,
       PointType setType
) {
}
