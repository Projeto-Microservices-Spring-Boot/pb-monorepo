package com.infnet.geolocalizacao.controller;




import com.infnet.geolocalizacao.dto.copaEstadios.MatchDTO;
import com.infnet.geolocalizacao.dto.trocaEventos.PlacesResponseDTO;
import com.infnet.geolocalizacao.services.ApiServices;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/maps")
@AllArgsConstructor
public class MapsApiController {

    private final ApiServices apiServices;

    @GetMapping("/estadios")
    public Mono<List<PlacesResponseDTO>> getEstadios(@RequestParam String endereco) {
        return apiServices.buscarLocais(endereco);
    }

    @GetMapping("/test-football-api")
    public ResponseEntity<?> testFootballApi() {
        List<MatchDTO> jogos = apiServices.buscarJogosCopas();
        return ResponseEntity.ok(jogos);
    }


    @GetMapping("/loc-estadios")
    public ResponseEntity<?> testBuscaEstadioLoc(@RequestParam String nome) {
        return  ResponseEntity.ok(apiServices.buscarPorNome(nome));
    }
}
