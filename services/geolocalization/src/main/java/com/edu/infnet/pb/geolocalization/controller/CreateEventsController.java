package com.infnet.geolocalizacao.controller;


import com.infnet.geolocalizacao.dto.trocaEventos.CreateEventsDTO;
import com.infnet.geolocalizacao.dto.trocaEventos.CreateTradeDTO;
import com.infnet.geolocalizacao.model.EventoCopa;
import com.infnet.geolocalizacao.model.PointMap;
import com.infnet.geolocalizacao.services.EventoCopaService;
import com.infnet.geolocalizacao.services.EventosServices;
import com.infnet.geolocalizacao.services.TrocasServices;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/criacao/maps")
public class CreateEventsController {

    private final EventoCopaService eventoCopa;

    private final EventosServices eventosServices;

    private final TrocasServices trocasServices;

    @PostMapping("/troca")
    public ResponseEntity<PointMap> pontoTroca(@RequestBody CreateTradeDTO createTradeDTO){
        return trocasServices.criarTroca(createTradeDTO);

    }

    @PostMapping("/evento")
    public ResponseEntity<PointMap> pontoEvento(@RequestBody CreateEventsDTO createEventsDTO){
        return eventosServices.criarEvento(createEventsDTO);
    }



    @GetMapping("/evento-copa")
    public ResponseEntity<List<EventoCopa>> listarEventosCopa() {
        return ResponseEntity.ok(
                eventoCopa.listarEventosCopa()
        );
    }

    @GetMapping("/eventos")
    public ResponseEntity<List<PointMap>> listarEventos() {
        return ResponseEntity.ok(
                eventosServices.listarEventos()
        );
    }

}
