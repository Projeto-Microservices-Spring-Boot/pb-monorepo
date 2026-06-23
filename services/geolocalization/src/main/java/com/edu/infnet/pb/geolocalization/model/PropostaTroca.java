package com.edu.infnet.pb.geolocalization.model;

import com.edu.infnet.pb.geolocalization.model.Enums.StatusTroca;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.util.UUID;

@Entity
@Getter @Setter
public class PropostaTroca {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

private UUID usuarioOrigem;
private UUID usuarioDestino;

@Enumerated(EnumType.STRING)
private StatusTroca status;

    @Lob
    private String stickersOrigemJson;   // JSON de List<FigurinhaTrocaDTO> que origem oferece

    @Lob
    private String stickersDestinoJson;  // JSON de List<FigurinhaTrocaDTO> que destino oferece
}
