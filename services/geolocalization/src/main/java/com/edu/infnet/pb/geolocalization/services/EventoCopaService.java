package com.infnet.geolocalizacao.services;




import com.infnet.geolocalizacao.dto.GeoLocalizacaoDosEtadiosDTO;
import com.infnet.geolocalizacao.dto.copaEstadios.MatchDTO;
import com.infnet.geolocalizacao.model.Enums.PointType;
import com.infnet.geolocalizacao.model.EventoCopa;
import com.infnet.geolocalizacao.repository.EventoCopaRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
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


    @PostConstruct
    public void init() {
        if (eventoCopaRepository.count() == 0) {
            criarEventosCopa();
        }
    }

    public ResponseEntity<EventoCopaService> criarEventosCopa() {

        List<MatchDTO> jogos = apiServices.buscarJogosCopas();

        // precisaria fazer uma formatacao de horario para jogar no banco porque o api nao aceita localdatetime so aceita String
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

        jogos.forEach(jogo -> {

            if (jogo.timeA() == null || jogo.timeB() == null) {
                System.out.println("Jogo sem seleção definida");
                System.out.println(jogo);
            }

          EventoCopa eventoCopa = new EventoCopa();

            String timeA = jogo.timeA() == null ? "A definir" : jogo.timeA();
            String timeB = jogo.timeB() == null ? "A definir" : jogo.timeB();
            String selecoes = timeA + " x " + timeB;

            eventoCopa.setNome(selecoes);
            eventoCopa.setEndereco(jogo.estadio());
            eventoCopa.setSelecoes(selecoes);
            eventoCopa.setEstadio(jogo.estadio());
            eventoCopa.setCategoria(jogo.grupo());
            eventoCopa.setTipo(PointType.EVENTO_COPA);
            eventoCopa.setDataJogo(
                    LocalDateTime.parse(jogo.dataJogo() , formatter)
            );


            GeoLocalizacaoDosEtadiosDTO locais = apiServices.buscarPorNome(jogo.estadio());
            System.out.println("Estádio API: " + jogo.estadio());
            System.out.println("Localização encontrada: " + locais);

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

    public ResponseEntity<Void> deletandoEventoCopas(Long id) {
        EventoCopa eventoCopa = eventoCopaRepository.findByIdAndTipo(id , PointType.EVENTO_COPA).orElseThrow(() -> new RuntimeException("Evento da copa nao encontrado "));
        eventoCopaRepository.delete(eventoCopa);

        return ResponseEntity.noContent().build();
    }


    public List<EventoCopa> listarEventosCopa() {
        return eventoCopaRepository.findAll();
    }

}
