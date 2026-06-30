package com.edu.infnet.pb.geolocalization.dto.trocaEventos;


import jakarta.validation.constraints.NotBlank;
import org.antlr.v4.runtime.misc.NotNull;

public record CreateTradeDTO(
        @NotBlank
        String nome,
        String descricao,
        @NotNull
        String endereco,
        Double latitude,
        Double longitude
) {
}
