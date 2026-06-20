package com.infnet.geolocalizacao.model;





import com.infnet.geolocalizacao.model.Enums.PointType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pontos_mapa")
@Inheritance(strategy = InheritanceType.JOINED) // tabela separada por subtipo
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PointMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome" , nullable = false)
    private String nome;

    @Column(name = "descricao" , nullable = true)
    private String descricao;

    @Column(name = "endereco" , nullable = false)
    private String endereco;

    @Column(name = "latitude" , nullable = false)
    private Double latitude;

    @Column(name = "longitude" , nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType tipo;

    @Column(nullable = true)
    private LocalDate dataInicioEvento;

    @Column(nullable = true)
    private LocalDate dataFimEventos;
}
