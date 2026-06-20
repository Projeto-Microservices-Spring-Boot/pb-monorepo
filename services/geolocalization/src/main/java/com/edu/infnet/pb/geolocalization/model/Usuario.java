package com.infnet.geolocalizacao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class Usuario {
    @Id
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column
    private String email;
}
