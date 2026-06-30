package com.edu.infnet.pb.geolocalization.dto;

import java.util.UUID;

public record PerfilResponseDTO(
        UUID id,
        String nome ,
        String email
) {
}
