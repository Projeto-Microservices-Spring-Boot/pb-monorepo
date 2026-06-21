package com.edu.infnet.pb.geolocalization.controller;


import com.edu.infnet.pb.geolocalization.dto.EditarEventosDTO;
import com.edu.infnet.pb.geolocalization.dto.TrocaDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CreateEventsDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CreateTradeDTO;
import com.edu.infnet.pb.geolocalization.model.EventoCopa;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.model.Eventos;
import com.edu.infnet.pb.geolocalization.model.Trocas;
import com.edu.infnet.pb.geolocalization.services.EventoCopaService;
import com.edu.infnet.pb.geolocalization.services.EventosServices;
import com.edu.infnet.pb.geolocalization.services.TrocasServices;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/maps")
public class LocalizacaoController {

    private final EventoCopaService eventoCopa;

    private final EventosServices eventosServices;

    private final TrocasServices trocasServices;


// ====================================================================================================================================//

    // Trocas

    @PostMapping("/troca/criar")
    public ResponseEntity<Trocas> pontoTroca(@RequestBody CreateTradeDTO createTradeDTO) {
        return trocasServices.criarTroca(createTradeDTO);
    }

    @PostMapping("/troca/favoritando")
    public ResponseEntity<Favorito> favoritarTroca(@RequestParam Long eventoId, @RequestHeader("Authorization") String token) {
        return trocasServices.favoritarTrocas(eventoId, token);
    }

    @DeleteMapping("/troca/deletando")
    public ResponseEntity<Trocas> deletandoTrocas(@RequestParam Long eventoId, @RequestHeader("Authorization") String token) {
        return trocasServices.deletandoTrocas(eventoId, token);
    }

    @PutMapping("/troca/editarTrocas")
    public ResponseEntity<Trocas> editarTrocas(@RequestParam Long eventoId, @RequestBody EditarEventosDTO editarEventosDTO) {
        return trocasServices.editarTroca(eventoId, editarEventosDTO);
    }

    @GetMapping("/trocas")
    public ResponseEntity<List<Trocas>> listarTrocas() {
        return ResponseEntity.ok(trocasServices.listarTrocas());
    }


    @GetMapping("/trocas/{trocaId}/matches")
    public ResponseEntity<List<TrocaDTO>> buscarMatchesNaTroca(
            @PathVariable Long trocaId,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(trocasServices.buscarMatchesNaTroca(trocaId, token));
    }


// ====================================================================================================================================//

    // Eventos

    @PostMapping("/criar/evento")
    public ResponseEntity<Eventos> pontoEvento(@Valid @RequestBody CreateEventsDTO createEventsDTO) {
        return eventosServices.criarEvento(createEventsDTO);
    }

    @GetMapping("/eventos")
    public ResponseEntity<List<Eventos>> listarEventos() {
        return ResponseEntity.ok(eventosServices.listarEventos());
    }

    @PostMapping("/evento/favoritando")
    public ResponseEntity<Favorito> favoritarEvento(@RequestParam Long eventoId, @RequestHeader("Authorization") String token) {
        return eventosServices.favoritarEvento(eventoId, token);
    }

    @DeleteMapping("/evento/deletando")
    public ResponseEntity<Eventos> deletandoEvento(@RequestParam Long eventoId) {
        return eventosServices.deletandoEvento(eventoId);
    }

    @PutMapping("/evento/editarEvento")
    public ResponseEntity<Eventos> editarEventos(@RequestParam Long eventoId, @RequestBody EditarEventosDTO editarEventosDTO) {
        return eventosServices.editarEvento(eventoId, editarEventosDTO);
    }

    @GetMapping("/eventos/proximos")
    public ResponseEntity<List<Eventos>> eventosProximos(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "10") Double raioKm) {
        return eventosServices.buscarEventosProximos(lat , lng , raioKm);
    }


    @GetMapping("/eventos/{eventoId}/matches")
    public ResponseEntity<List<TrocaDTO>> buscarMatchesNoEvento(
            @PathVariable Long eventoId,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(eventosServices.buscarMatchesNoEvento(eventoId, token));
    }


// ====================================================================================================================================//

    // Eventos Copa

    @GetMapping("/evento-copa")
    public ResponseEntity<List<EventoCopa>> listarEventosCopa() {
        return ResponseEntity.ok(eventoCopa.listarEventosCopa());
    }

    @PostMapping("/evento-copa/favoritando/{eventoId}")
    public ResponseEntity<Favorito> favoritoResponseEntity(@PathVariable Long eventoId, @RequestHeader("Authorization") String token) {
        return eventoCopa.favoritarEventoCopa(eventoId, token);
    }

    @GetMapping("/evento-copa/favoritando/listar")
    public ResponseEntity<List<Favorito>> listarFavoritoResponse(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(eventoCopa.listarFavoritos(token));
    }


    @DeleteMapping("/evento-copa/favoritando/removendo/{eventoId}")
    public ResponseEntity<Void> removendofavoritoResponse(@PathVariable Long eventoId, @RequestHeader("Authorization") String token) {
        return eventoCopa.deletandoFavoritosCopa(eventoId, token);
    }

// ====================================================================================================================================//

}