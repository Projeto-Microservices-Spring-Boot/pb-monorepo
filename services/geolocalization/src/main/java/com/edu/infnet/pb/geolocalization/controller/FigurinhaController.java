package com.edu.infnet.pb.geolocalization.controller;

import com.edu.infnet.pb.geolocalization.dto.figurinhas.TradeMatchResponseDTO;
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

    @GetMapping("/usuario/matches")
    public ResponseEntity<List<TradeMatchResponseDTO>> buscarMeusMatches(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(figurinhaService.buscarMeusMatches(token));
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