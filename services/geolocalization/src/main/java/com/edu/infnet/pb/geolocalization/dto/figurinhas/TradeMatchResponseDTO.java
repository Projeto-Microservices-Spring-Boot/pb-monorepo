package com.edu.infnet.pb.geolocalization.dto.figurinhas;

import java.util.List;
import java.util.UUID;

public record TradeMatchResponse(
        UUID userIda,
        UUID userIdB,
        int score,
        int quantidadeFigurinhasRepetidasUsuarioA ,
        int quantidadeFigurinhasRepetidasUsuarioB ,
        List<FigurinhaDTO> nomeDosJogadores
) {
}
