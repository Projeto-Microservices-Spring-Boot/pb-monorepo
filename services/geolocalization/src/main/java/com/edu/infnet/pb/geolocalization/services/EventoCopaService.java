package com.edu.infnet.pb.geolocalization.services;


import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.GeoLocalizacaoDosEtadiosDTO;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.copaEstadios.MatchDTO;
import com.edu.infnet.pb.geolocalization.model.EventoCopa;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.repository.EventoCopaRepository;
import com.edu.infnet.pb.geolocalization.repository.FavoritoRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor
public class EventoCopaService {

    private final EventoCopaRepository eventoCopaRepository;
    private final ApiServices apiServices;
    private final UsuarioClient usuarioClient;
    private final FavoritoRepository favoritoRepository;


    @PostConstruct
    public void init() {
        try {
            if (eventoCopaRepository.count() == 0) {
                System.out.println("acesso ao api ");
                criarEventosCopa();
            }
        } catch (Exception e) {
            // Se a API externa falhar, não quebra a aplicação
            System.out.println("API externa indisponível ao iniciar: " + e.getMessage());
        }
    }

    public ResponseEntity<EventoCopaService> criarEventosCopa() {

        List<MatchDTO> jogos = apiServices.buscarJogosCopas();

        // precisaria fazer uma formatacao de horario para jogar no banco porque o api nao aceita localdatetime so aceita String
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

        jogos.forEach(jogo -> {

          EventoCopa eventoCopa = new EventoCopa();
            String timeA = jogo.timeA() == null ? "A definir" : jogo.timeA();
            String timeB = jogo.timeB() == null ? "A definir" : jogo.timeB();
            String selecoes = timeA + " x " + timeB;

            eventoCopa.setNome(selecoes);
            eventoCopa.setEndereco(jogo.estadio());
            eventoCopa.setSelecoes(selecoes);
            eventoCopa.setEstadio(jogo.estadio());
            eventoCopa.setCategoria(jogo.grupo());
            eventoCopa.setDataJogo(
                    LocalDateTime.parse(jogo.dataJogo() , formatter)
            );


            GeoLocalizacaoDosEtadiosDTO locais = apiServices.buscarPorNome(jogo.estadio());

            if (locais != null) {
                eventoCopa.setLatitude(locais.latitude());
                eventoCopa.setLongitude(locais.longitude());
            }
            else {
                eventoCopa.setLatitude(0.0);
                eventoCopa.setLongitude(0.0);
            }
            eventoCopaRepository.save(eventoCopa);
        });

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Favorito> favoritarEventoCopa (Long eventoId , String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        EventoCopa evento = eventoCopaRepository.findById(eventoId).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        Favorito favorito = Favorito.builder()
                .usuarioId(usuario.id())
                .eventoCopa(evento)
                .dataFavoritado(LocalDateTime.now())
                .build();

        favoritoRepository.save(favorito);

        return ResponseEntity.status(HttpStatus.CREATED).body(favorito);

    }

    public ResponseEntity<Void> deletandoFavoritosCopa(Long eventoId , String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        Favorito favorito = favoritoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Favorito não encontrado"));

        if (!favorito.getUsuarioId().equals(usuario.id())) {
            throw new RuntimeException("Favorito não pertence ao usuário");
        }

        favoritoRepository.delete(favorito);

        return ResponseEntity.noContent().build();
    }

    public List<Favorito> listarFavoritos(String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
        return favoritoRepository.findByUsuarioId(usuario.id());
    }


    public List<EventoCopa> listarEventosCopa() {
        return eventoCopaRepository.findAll();
    }

}
