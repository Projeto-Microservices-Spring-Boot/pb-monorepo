package com.edu.infnet.pb.geolocalization.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trocas")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class Trocas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;
    private String endereco;
    private Double latitude;
    private Double longitude;
    private UUID criadorId;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

}