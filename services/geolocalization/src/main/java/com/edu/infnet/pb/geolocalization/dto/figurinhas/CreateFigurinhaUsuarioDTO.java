package com.edu.infnet.pb.geolocalization.dto.figurinhas;

import com.edu.infnet.pb.geolocalization.model.Enums.StatusFigurinha;

public record CreateFigurinhaUsuarioDTO(
        Long figurinhaId ,
        StatusFigurinha status,
        Integer quantidade
) {
}
