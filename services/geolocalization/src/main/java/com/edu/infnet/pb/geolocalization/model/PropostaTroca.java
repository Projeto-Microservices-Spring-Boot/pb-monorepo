package com.edu.infnet.pb.geolocalization.model;

import com.edu.infnet.pb.geolocalization.model.Enums.StatusTroca;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class PropostaTroca {

@Id
@GeneratedValue
private Long id;

private UUID usuarioOrigem;

private UUID usuarioDestino;

@Enumerated(EnumType.STRING)
private StatusTroca status;


@ElementCollection
@CollectionTable(name = "proposta_troca_figurinhas_origem", joinColumns = @JoinColumn(name = "proposta_troca_id"))
@Column(name = "figurinha_id")
private List<Long> figurinhasQueOrigemRecebe = new ArrayList<>();

@ElementCollection
@CollectionTable(name = "proposta_troca_figurinhas_destino", joinColumns = @JoinColumn(name = "proposta_troca_id"))
@Column(name = "figurinha_id")
private List<Long> figurinhasQueDestinoRecebe = new ArrayList<>();

}
