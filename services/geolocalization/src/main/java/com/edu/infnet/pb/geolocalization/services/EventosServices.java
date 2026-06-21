package com.edu.infnet.pb.geolocalization.services;


import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.EditarEventosDTO;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.TrocaDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CreateEventsDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.PlacesResponseDTO;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.model.Eventos;
import com.edu.infnet.pb.geolocalization.repository.EventoRepository;
import com.edu.infnet.pb.geolocalization.repository.FavoritoRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EventosServices {

private ApiServices apiServices;
private UsuarioClient usuarioClient;
private FavoritoRepository favoritoRepository;
private EventoRepository eventoRepository;
private FigurinhaService figurinhaService;

    public ResponseEntity<Eventos> criarEvento(CreateEventsDTO createEventsDTO) {
        Eventos eventos = new Eventos();

        eventos.setNome(createEventsDTO.nome());
        eventos.setDescricao(createEventsDTO.descricao());
        eventos.setEndereco(createEventsDTO.endereco());
        eventos.setLatitude(createEventsDTO.latitude());
        eventos.setLongitude(createEventsDTO.longitude());
        eventos.setDataInicioEvento(createEventsDTO.dataInicio());
        eventos.setDataFimEventos(createEventsDTO.dataFim());

        if (createEventsDTO.latitude() == null || createEventsDTO.longitude() == null) {
            List<PlacesResponseDTO> locais = apiServices.buscarLocais(createEventsDTO.endereco()).block();
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

        return ResponseEntity.ok(salvo);
    }

    public ResponseEntity<Eventos> deletandoEvento(Long id) {

    Eventos eventos = eventoRepository.findById(id).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

    favoritoRepository.deleteByEventoId(id);
    eventoRepository.delete(eventos);

    return ResponseEntity.noContent().build();
    }


    public ResponseEntity<Eventos> editarEvento(Long id, EditarEventosDTO editarEventosDTO) {
        Eventos eventos = eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        eventos.setDataInicioEvento(editarEventosDTO.dataInicio());
        eventos.setDataFimEventos(editarEventosDTO.dataFim());

        return ResponseEntity.ok(eventoRepository.save(eventos));

    }


    public ResponseEntity<Favorito> favoritarEvento (Long eventoId , String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

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
        List<Eventos> eventos = eventoRepository.buscarEventosProximos(lat, lng, raioKm);
        return  ResponseEntity.ok(eventos);
    }

    public List<TrocaDTO> buscarMatchesNoEvento(Long eventoId, String token) {
        List<TrocaDTO> todosMatches = figurinhaService.buscarMatches(token);
        List<UUID> usuariosNoEvento = favoritoRepository.buscarUsuarioIdsPorEvento(eventoId);
        return figurinhaService.filtrarMatchesPorUsuarios(todosMatches, usuariosNoEvento);
    }


    public List<Eventos> listarEventos() {
        return eventoRepository.findAll();
    }


}
