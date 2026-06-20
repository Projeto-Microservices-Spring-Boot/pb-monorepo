package com.infnet.geolocalizacao.dto.copaEstadios;

import java.util.List;

public record FootballResponseDTO(
        List<MatchDTO> games
) {
}
