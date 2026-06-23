package com.edu.infnet.pb.geolocalization.Mock;

import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class UsuarioClientMock implements UsuarioClient {

    private final Map<String, PerfilResponseDTO> usuariosFake = Map.of(
            "user1", new PerfilResponseDTO(
                    UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                    "Usuario Um",
                    "user1@email.com"
            ),
            "user2", new PerfilResponseDTO(
                    UUID.fromString("223e4567-e89b-12d3-a456-426614174001"),
                    "Usuario Dois",
                    "user2@email.com"
            ),
            "user3", new PerfilResponseDTO(
                    UUID.fromString("323e4567-e89b-12d3-a456-426614174002"),
                    "Usuario Tres",
                    "user3@email.com"
            )
    );

    @Override
    public PerfilResponseDTO buscarUusarioLogado(String token) {
        String tokenLimpo = token.replace("Bearer ", "").trim();

        return usuariosFake.getOrDefault(tokenLimpo,
                new PerfilResponseDTO(
                        UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                        "Usuario Teste",
                        "teste@email.com"
                )
        );
    }
}