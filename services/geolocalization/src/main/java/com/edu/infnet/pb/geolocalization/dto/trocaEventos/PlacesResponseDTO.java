package com.infnet.geolocalizacao.dto.trocaEventos;


public record PlacesResponseDTO(
        Long place_id ,
        String display_name ,
        String lat,
        String lon
) { }
