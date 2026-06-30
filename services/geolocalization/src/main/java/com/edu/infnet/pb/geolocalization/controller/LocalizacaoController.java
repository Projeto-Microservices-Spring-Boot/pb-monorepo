package com.edu.infnet.pb.geolocalization.controller;


import com.edu.infnet.pb.geolocalization.dto.EditarEventosDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CriacaoEventoDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CreateTradeDTO;
import com.edu.infnet.pb.geolocalization.model.EventoCopa;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.model.Eventos;
import com.edu.infnet.pb.geolocalization.model.PontosTrocas;
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
    public ResponseEntity<PontosTrocas> pontoTroca(@RequestBody CreateTradeDTO createTradeDTO ,@RequestHeader("Authorization") String token) {
        return trocasServices.criarTroca(createTradeDTO , token);
    }

    @PostMapping("/troca/favoritando")
    public ResponseEntity<Favorito> favoritarTroca(@RequestParam Long eventoId, @RequestHeader("Authorization") String token) {
        return trocasServices.favoritarTrocas(eventoId, token);
    }

    @DeleteMapping("/troca/deletando")
    public ResponseEntity<PontosTrocas> deletandoTrocas(@RequestParam Long eventoId, @RequestHeader("Authorization") String token) {
        return trocasServices.deletandoTrocas(eventoId, token);
    }

    @PutMapping("/troca/editarTrocas")
    public ResponseEntity<PontosTrocas> editarTrocas(@RequestParam Long eventoId, @RequestBody EditarEventosDTO editarEventosDTO) {
        return trocasServices.editarTroca(eventoId, editarEventosDTO);
    }

    @GetMapping("/trocas")
    public ResponseEntity<List<PontosTrocas>> listarTrocas() {
        return ResponseEntity.ok(trocasServices.listarTrocas());
    }


    @GetMapping("/trocas/buscar-por-id/{id}")
    public ResponseEntity<PontosTrocas> findById(@PathVariable Long id) {
        return trocasServices.findById(id);
    }

// ====================================================================================================================================//

    // Eventos

    @PostMapping("/criar/evento")
    public ResponseEntity<Eventos> pontoEvento(@Valid @RequestBody CriacaoEventoDTO criacaoEventoDTO) {
        return eventosServices.criarEvento(criacaoEventoDTO);
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