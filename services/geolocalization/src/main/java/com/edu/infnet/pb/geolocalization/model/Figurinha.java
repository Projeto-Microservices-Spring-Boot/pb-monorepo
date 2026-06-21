package com.edu.infnet.pb.geolocalization.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "figurinhas")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class Figurinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false, unique = true)
    private String jogador;

    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String categoria;

}
