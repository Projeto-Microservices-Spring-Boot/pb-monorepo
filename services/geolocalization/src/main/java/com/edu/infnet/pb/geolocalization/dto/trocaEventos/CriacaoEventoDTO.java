package com.edu.infnet.pb.geolocalization.dto.trocaEventos;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CriacaoEventoDTO(
        @NotBlank
        String nome,
        String descricao,
        @NotBlank
        String endereco,
        Double latitude,
        Double longitude,
        LocalDateTime dataInicio,
        LocalDateTime dataFim
) {
}