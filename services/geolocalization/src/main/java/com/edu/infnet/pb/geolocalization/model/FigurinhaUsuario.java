package com.edu.infnet.pb.geolocalization.model;

import com.edu.infnet.pb.geolocalization.model.Enums.StatusFigurinha;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "figurinhas_usuario")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class FigurinhaUsuario {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(nullable = false)
private UUID usuarioId;

@ManyToOne
@JoinColumn(name = "figurinha_id" , nullable = false)
private Figurinha figurinha;

@Enumerated(EnumType.STRING)
@Column(nullable = false)
private StatusFigurinha status;

@Column(nullable = false)
private Integer quantidade;

}
