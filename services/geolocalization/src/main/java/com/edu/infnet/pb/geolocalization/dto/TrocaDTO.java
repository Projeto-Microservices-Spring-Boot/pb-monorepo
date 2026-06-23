package com.edu.infnet.pb.geolocalization.dto;

import com.edu.infnet.pb.geolocalization.dto.figurinhas.FigurinhaDTO;

import java.util.List;
import java.util.UUID;

public record TrocaDTO(

        UUID usuarioId,

        Integer score,

        Integer quantidadeTenho,

        Integer quantidadeEleTem,

        List<FigurinhaDTO> eleTemQueEuPreciso

) {
}
