package com.edu.infnet.pb.geolocalization.services;


import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.EditarEventosDTO;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CriacaoEventoDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.PlacesResponseDTO;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.model.Eventos;
import com.edu.infnet.pb.geolocalization.repository.EventoRepository;
import com.edu.infnet.pb.geolocalization.repository.FavoritoRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class EventosServices {

private ApiServices apiServices;
private UsuarioClient usuarioClient;
private FavoritoRepository favoritoRepository;
private EventoRepository eventoRepository;


private static final Logger log =
        LogManager.getLogger(EventosServices.class);


    public ResponseEntity<Eventos> criarEvento(CriacaoEventoDTO criacaoEventoDTO) {

        log.info(
                "Criando evento nome={} endereco={}",
                criacaoEventoDTO.nome(),
                criacaoEventoDTO.endereco()
        );

        Eventos eventos = new Eventos();

        eventos.setNome(criacaoEventoDTO.nome());
        eventos.setDescricao(criacaoEventoDTO.descricao());
        eventos.setEndereco(criacaoEventoDTO.endereco());
        eventos.setLatitude(criacaoEventoDTO.latitude());
        eventos.setLongitude(criacaoEventoDTO.longitude());
        eventos.setDataInicioEvento(criacaoEventoDTO.dataInicio());
        eventos.setDataFimEventos(criacaoEventoDTO.dataFim());

        if (criacaoEventoDTO.latitude() == null || criacaoEventoDTO.longitude() == null) {

            log.info(
                    "Buscando coordenadas automaticamente endereco={}",
                    criacaoEventoDTO.endereco()
            );

            List<PlacesResponseDTO> locais = apiServices.buscarLocais(criacaoEventoDTO.endereco()).block();
            if(locais != null && !locais.isEmpty()) {
                PlacesResponseDTO local = locais.get(0);
                eventos.setLatitude(
                        Double.parseDouble(local.lat())
                );
                eventos.setLongitude(
                        Double.parseDouble(local.lon())
                );
            }
        }

        Eventos salvo = eventoRepository.save(eventos);


        log.info(
                "Evento criado com sucesso eventoId={}",
                salvo.getId()
        );

        return ResponseEntity.ok(salvo);
    }

    public ResponseEntity<Eventos> deletandoEvento(Long id) {

        log.info(
                "Solicitada exclusao do evento eventoId={}",
                id
        );

    Eventos eventos = eventoRepository.findById(id).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

    favoritoRepository.deleteByEventoId(id);
    eventoRepository.delete(eventos);


        log.info(
                "Evento removido com sucesso eventoId={}",
                id
        );


        return ResponseEntity.noContent().build();
    }


    public ResponseEntity<Eventos> editarEvento(Long id, EditarEventosDTO editarEventosDTO) {


        log.info(
                "Editando evento eventoId={}",
                id
        );

        Eventos eventos = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        eventos.setDataInicioEvento(editarEventosDTO.dataInicio());
        eventos.setDataFimEventos(editarEventosDTO.dataFim());


        return ResponseEntity.ok(eventoRepository.save(eventos));

    }

    public ResponseEntity<Favorito> favoritarEvento (Long eventoId , String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        log.info(
                "Favoritando evento eventoId={} usuarioId={}",
                eventoId,
                usuario.id()
        );


        Eventos evento = eventoRepository.findById(eventoId).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        Favorito favorito = Favorito.builder()
                .usuarioId(usuario.id())
                .evento(evento)
                .dataFavoritado(LocalDateTime.now())
                .build();

        favoritoRepository.save(favorito);

        return ResponseEntity.status(HttpStatus.CREATED).body(favorito);

    }

    public ResponseEntity<List<Eventos>> buscarEventosProximos(Double lat , Double lng , Double raioKm) {

        log.info(
                "Buscando eventos proximos lat={} lng={} raioKm={}",
                lat,
                lng,
                raioKm
        );

        List<Eventos> eventos = eventoRepository.buscarEventosProximos(lat, lng, raioKm);
        return  ResponseEntity.ok(eventos);
    }

    public List<Eventos> listarEventos() {
        log.info("Listando todos os eventos");
        return eventoRepository.findAll();
    }
}
