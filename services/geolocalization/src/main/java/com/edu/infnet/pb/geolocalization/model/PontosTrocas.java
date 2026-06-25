package com.edu.infnet.pb.geolocalization.model;

import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "trocas")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class PontosTrocas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuarioId")
    private UUID criadorId;

    @Column(name = "nome" , nullable = false)
    private String nome;

    @Column(name = "descricao" , nullable = true)
    private String descricao;

    @Column(name = "endereco" , nullable = false)
    private String endereco;

    @Column(name = "latitude" , nullable = true)
    private Double latitude;

    @Column(name = "longitude" , nullable = true)
    private Double longitude;
}
