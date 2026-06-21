package com.edu.infnet.pb.geolocalization.controller;

import com.edu.infnet.pb.geolocalization.dto.TrocaDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.CreateFigurinhaDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.CreateFigurinhaUsuarioDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.EditarQuantidadeDTO;
import com.edu.infnet.pb.geolocalization.model.Figurinha;
import com.edu.infnet.pb.geolocalization.model.FigurinhaUsuario;
import com.edu.infnet.pb.geolocalization.model.PropostaTroca;
import com.edu.infnet.pb.geolocalization.services.FigurinhaService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/figurinhas")
public class FigurinhaController {

    private final FigurinhaService figurinhaService;

    @PostMapping
    public ResponseEntity<Figurinha> criarFigurinha(@RequestBody CreateFigurinhaDTO dto) {
        return figurinhaService.criarFigurinha(dto);
    }

    @GetMapping
    public ResponseEntity<List<Figurinha>> listarFigurinhas() {
        return ResponseEntity.ok(figurinhaService.listarFigurinhas());
    }

    @PostMapping("/usuario")
    public ResponseEntity<FigurinhaUsuario> adicionarFigurinha(
            @RequestBody CreateFigurinhaUsuarioDTO dto,
            @RequestHeader("Authorization") String token) {
        return figurinhaService.adicionarFigurinha(dto, token);
    }

    @GetMapping("/usuario/faltantes")
    public ResponseEntity<List<FigurinhaUsuario>> listarMinhasFaltantes(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(figurinhaService.listarMinhasFaltantes(token));
    }

    @GetMapping("/usuario/repetidas")
    public ResponseEntity<List<FigurinhaUsuario>> listarMinhasRepetidas(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(figurinhaService.listarMinhasRepetidas(token));
    }

    @PutMapping("/usuario/{id}")
    public ResponseEntity<FigurinhaUsuario> editarQuantidade(
            @PathVariable Long id,
            @RequestBody EditarQuantidadeDTO dto,
            @RequestHeader("Authorization") String token) {
        return figurinhaService.editarQuantidade(id, dto, token);
    }

    @DeleteMapping("/usuario/{id}")
    public ResponseEntity<Void> removerFigurinhaUsuario(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return figurinhaService.removerFigurinhaUsuario(id, token);
    }


    @GetMapping("/usuario/matches")
    public ResponseEntity<List<TrocaDTO>> buscarMatches(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(figurinhaService.buscarMatches(token));
    }


    @PostMapping("/trocas/propor/{destino}")
    public ResponseEntity<PropostaTroca> enviarProposta(
            @PathVariable UUID destino,
            @RequestHeader("Authorization") String token) {
        return figurinhaService.enviarProposta(destino, token);
    }

    @PutMapping("/trocas/{id}/aceitar")
    public ResponseEntity<PropostaTroca> aceitarProposta(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return figurinhaService.aceitarProposta(id, token);
    }

    @PutMapping("/trocas/{id}/rejeitar")
    public ResponseEntity<PropostaTroca> rejeitarProposta(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return figurinhaService.rejeitarProposta(id, token);
    }

}