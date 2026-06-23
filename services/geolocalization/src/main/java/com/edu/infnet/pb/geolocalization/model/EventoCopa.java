package com.edu.infnet.pb.geolocalization.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "eventos_copa")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EventoCopa{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String endereco;
    private Double latitude;
    private Double longitude;
    @Column(nullable = false)
    private String selecoes;
    @Column(nullable = false)
    private String categoria;
    @Column(nullable = false)
    private LocalDateTime dataJogo;
    private String estadio;
    private boolean ingresosDisponiveis;
}
