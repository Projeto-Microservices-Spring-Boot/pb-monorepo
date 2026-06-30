package com.edu.infnet.pb.geolocalization.model;




import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pontos_mapa")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Eventos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = true)
    private LocalDateTime dataInicioEvento;

    @Column(nullable = true)
    private LocalDateTime dataFimEventos;
}
