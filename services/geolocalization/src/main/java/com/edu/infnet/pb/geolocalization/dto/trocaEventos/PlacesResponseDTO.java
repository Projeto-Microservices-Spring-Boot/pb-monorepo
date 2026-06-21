package com.edu.infnet.pb.geolocalization.dto.trocaEventos;


public record PlacesResponseDTO(
        Long place_id ,
        String display_name ,
        String lat,
        String lon
) { }
