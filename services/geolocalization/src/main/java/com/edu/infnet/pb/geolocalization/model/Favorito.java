package com.edu.infnet.pb.geolocalization.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "favoritos")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Builder
public class Favorito {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(nullable = false)
    private UUID usuarioId;

    @ManyToOne @JoinColumn(name = "evento_copa_id", nullable = true)
    private EventoCopa eventoCopa;

    @ManyToOne @JoinColumn(name = "evento_id", nullable = true)
    private Eventos evento;

    @ManyToOne @JoinColumn(name = "troca_id", nullable = true)
    private Trocas troca;

    @Column(nullable = false)
    private LocalDateTime dataFavoritado;

}
