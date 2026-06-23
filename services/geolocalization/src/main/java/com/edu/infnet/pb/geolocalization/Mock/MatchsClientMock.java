package com.edu.infnet.pb.geolocalization.Mock;

import com.edu.infnet.pb.geolocalization.client.MatchsClient;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.FigurinhaDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.TradeMatchResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MatchsClientMock implements MatchsClient {

    @Override
    public List<TradeMatchResponseDTO> buscarMatches() {
        UUID usuarioA = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID usuarioB = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

        return List.of(
                new TradeMatchResponseDTO(
                        usuarioA,
                        usuarioB,
                        10,
                        3,
                        2,
                        List.of(
                                new FigurinhaDTO(1L, "Neymar"),
                                new FigurinhaDTO(2L, "Messi")
                        ),
                        List.of(
                                new FigurinhaDTO(3L, "Mbappé"),
                                new FigurinhaDTO(4L, "Vinícius Jr")
                        )
                )
        );
    }
}