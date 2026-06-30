package com.edu.infnet.pb.geolocalization.dto;

import java.time.LocalDateTime;

public record EditarEventosDTO(
        LocalDateTime dataInicio ,
        LocalDateTime dataFim
) {
}
