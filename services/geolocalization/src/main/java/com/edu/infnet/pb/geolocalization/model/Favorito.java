package com.infnet.geolocalizacao.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "favoritos")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Builder
public class Favorito {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
