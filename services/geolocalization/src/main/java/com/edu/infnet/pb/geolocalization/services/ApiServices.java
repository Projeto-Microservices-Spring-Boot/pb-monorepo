package com.edu.infnet.pb.geolocalization.services;


import com.edu.infnet.pb.geolocalization.dto.GeoLocalizacaoDosEtadiosDTO;
import com.edu.infnet.pb.geolocalization.dto.copaEstadios.FootballResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.copaEstadios.MatchDTO;
import com.edu.infnet.pb.geolocalization.dto.copaEstadios.StadiumDTO;
import com.edu.infnet.pb.geolocalization.dto.copaEstadios.StadiumResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.PlacesResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ApiServices {

    private final ObjectMapper objectMapper;
    private Map<String , GeoLocalizacaoDosEtadiosDTO> estadios;


    // buscar a lat e long
    private final WebClient nominatim =
            WebClient.builder()
                    .baseUrl("https://nominatim.openstreetmap.org")
                    .defaultHeader("User-Agent" , "ProjetoInfnet/1.0")
                    .build();

    @CircuitBreaker(name = "placesApi", fallbackMethod = "fallbackMap")
    public Mono<List<PlacesResponseDTO>> buscarLocais(String endereco) {
        return nominatim.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", endereco)
                        .queryParam("format", "jsonv2")
                        .build())
                .retrieve()
                .bodyToFlux(PlacesResponseDTO.class)
                .collectList();
    }


    public Mono<List<PlacesResponseDTO>> fallbackMap(String endereco , Throwable t) {
        System.out.println("Fallback acionado para: "+ endereco);
        return Mono.just(List.of());
    }



// buscar os eventos da copa
    private final WebClient football = WebClient.builder()
        .baseUrl("https://worldcup26.ir")
        .build();

    public List<MatchDTO> buscarJogosCopas() {


                List<MatchDTO>  jogos = football.get()
                .uri("/get/games")
                .retrieve()
                .bodyToMono(FootballResponseDTO.class)
                .map(FootballResponseDTO::games)
                .block();


                List<StadiumDTO> estadios = football.get()
                        .uri("/get/stadiums")
                        .retrieve()
                        .bodyToMono(StadiumResponseDTO.class)
                        .map(StadiumResponseDTO::stadiums)
                        .block();

            Map<String , String> estadioMap = estadios.stream()
                    .collect(Collectors.toMap(StadiumDTO::id , StadiumDTO::nome));

        return jogos.stream()
                .map(jogo -> new MatchDTO(
                        jogo.id(),
                        jogo.timeA(),
                        jogo.timeB(),
                        jogo.grupo(),
                        jogo.dataJogo(),
                        jogo.tipo(),
                        jogo.estadioId(),
                        estadioMap.get(jogo.estadioId()) // nome do estadio
                ))
                .toList();
    }



    // long e lati dos estadios
    @PostConstruct
    void carregarEstadios() throws IOException {

        try (InputStream is = getClass().getResourceAsStream("/localizacaoEstadio.json")) {


            if (is == null) {
                throw new RuntimeException(
                        "Arquivo localizacaoEstadio.json nao encontrado"
                );
            }


            estadios = Arrays.stream(
                    objectMapper.readValue(is , GeoLocalizacaoDosEtadiosDTO[].class)

            )
                    .collect(Collectors.toMap(
                            estadios -> estadios.nome().toLowerCase(),
                            Function.identity()
                    ));
        }
    }
    public GeoLocalizacaoDosEtadiosDTO buscarPorNome(String nome) {
        return estadios.get(nome.toLowerCase());
    }

}
