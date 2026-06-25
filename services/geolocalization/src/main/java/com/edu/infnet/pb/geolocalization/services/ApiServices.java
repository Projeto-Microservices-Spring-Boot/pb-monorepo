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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ApiServices {

    private static final Logger log = LogManager.getLogger(ApiServices.class);

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

        log.info("Buscando as localizacoes para endereco={}", endereco);

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
        log.warn(
                "Nenhuma localizacao encontrada endereco={} erro={}", endereco , t.getMessage());
        return Mono.just(List.of());
    }

    // buscar os eventos da copa
    private final WebClient football = WebClient.builder()
            .baseUrl("https://worldcup26.ir")
            .build();

    @CircuitBreaker(name = "footballApi", fallbackMethod = "fallbackBuscarJogosCopas")
    public List<MatchDTO> buscarJogosCopas() {
        log.info("Iniciando consulta dos jogos da copa");

        List<MatchDTO>  jogos = football.get()
                .uri("/get/games")
                .retrieve()
                .bodyToMono(FootballResponseDTO.class)
                .map(FootballResponseDTO::games)
                .timeout(Duration.ofSeconds(5))
                .block();


        List<StadiumDTO> estadios = football.get()
                .uri("/get/stadiums")
                .retrieve()
                .bodyToMono(StadiumResponseDTO.class)
                .map(StadiumResponseDTO::stadiums)
                .timeout(Duration.ofSeconds(5))
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

    public List<MatchDTO> fallbackBuscarJogosCopas(Throwable t) {
        log.error("Fallback acionado para buscarJogosCopas, erro={}", t.getMessage());
        return List.of();
    }

    // long e lati dos estadios
    @PostConstruct
    void carregarEstadios() throws IOException {
        log.info("carregando estadios!");
        try (InputStream is = getClass().getResourceAsStream("/localizacaoEstadio.json")) {


            if (is == null) {
                log.error("Arquivo localizacaoEstadio.json nao encontrado");
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

            log.info("Estadios carregados quantidade={}" , estadios.size());
        }
    }

    public GeoLocalizacaoDosEtadiosDTO buscarPorNome(String nome) {

        log.info("buscando estadios por nome = {}" , nome);
        return estadios.get(nome.toLowerCase());
    }

}