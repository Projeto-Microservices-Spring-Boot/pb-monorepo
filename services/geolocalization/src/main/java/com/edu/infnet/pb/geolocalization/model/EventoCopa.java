package com.infnet.geolocalizacao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "eventos_copa")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EventoCopa extends PointMap {
    @Column(nullable = false)
    private String selecoes;
    @Column(nullable = false)
    private String categoria;
    @Column(nullable = false)
    private LocalDateTime dataJogo;
    private String estadio;
    private boolean ingresosDisponiveis;
}
