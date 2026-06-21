package com.edu.infnet.pb.geolocalization.services;

import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.EditarEventosDTO;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.TrocaDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CreateTradeDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.PlacesResponseDTO;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.model.Trocas;
import com.edu.infnet.pb.geolocalization.repository.FavoritoRepository;
import com.edu.infnet.pb.geolocalization.repository.TrocaRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TrocasServices {

private final ApiServices apiServices;
private final FavoritoRepository favoritoRepository;
private final UsuarioClient usuarioClient;
private final TrocaRepository trocaRepository;
private final FigurinhaService figurinhaService;

    public ResponseEntity<Trocas> criarTroca(CreateTradeDTO createTradeDTO) {
        Trocas trocas = new Trocas();

        trocas.setNome(createTradeDTO.nome());
        trocas.setDescricao(createTradeDTO.descricao());
        trocas.setLatitude(createTradeDTO.latitude());
        trocas.setLongitude(createTradeDTO.longitude());
        trocas.setEndereco(createTradeDTO.endereco());


        if (createTradeDTO.latitude() == null ||
                createTradeDTO.longitude() == null) {
            List<PlacesResponseDTO> locais = apiServices.buscarLocais(createTradeDTO.endereco()).block();

            if (locais != null && !locais.isEmpty()){
                PlacesResponseDTO local = locais.get(0);

                trocas.setLatitude(
                        Double.parseDouble(local.lat())
                );
                trocas.setLongitude(
                        Double.parseDouble(local.lon())
                );
            }

        }

        return ResponseEntity.ok(trocaRepository.save(trocas));
    }


    public ResponseEntity<Trocas> deletandoTrocas(Long id , String token) {

        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        Trocas trocas = trocaRepository.findById(id).orElseThrow(() -> new RuntimeException("trocas nao encontrado"));

        if (!trocas.getCriadorId().equals(usuario.id())) {
            throw new RuntimeException("Apenas o criador pode deletar a troca");
        }

        favoritoRepository.deleteByTrocaId(id);
        trocaRepository.delete(trocas);

        return ResponseEntity.noContent().build();
    }


    public ResponseEntity<Trocas> editarTroca(Long id, EditarEventosDTO editarEventosDTO) {
        Trocas trocas = trocaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        trocas.setDataInicio(editarEventosDTO.dataInicio());
        trocas.setDataFim(editarEventosDTO.dataFim());

        return ResponseEntity.ok(trocaRepository.save(trocas));

    }


    public ResponseEntity<Favorito> favoritarTrocas (Long eventoId , String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        Trocas trocas = trocaRepository.findById(eventoId).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        Favorito favorito = Favorito.builder()
                .usuarioId(usuario.id())
                .troca(trocas)
                .dataFavoritado(LocalDateTime.now())
                .build();

        favoritoRepository.save(favorito);

        return ResponseEntity.status(HttpStatus.CREATED).body(favorito);

    }


    public List<TrocaDTO> buscarMatchesNaTroca(Long trocaId, String token) {
        List<TrocaDTO> todosMatches = figurinhaService.buscarMatches(token);
        List<UUID> usuariosNaTroca = favoritoRepository.buscarUsuarioIdsPorTroca(trocaId);
        return figurinhaService.filtrarMatchesPorUsuarios(todosMatches, usuariosNaTroca);
    }


    public List<Trocas> listarTrocas() {
        return trocaRepository.findAll();
    }




}


