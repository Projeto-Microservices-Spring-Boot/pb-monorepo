package com.edu.infnet.pb.geolocalization.services;

import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.EditarEventosDTO;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.CreateTradeDTO;
import com.edu.infnet.pb.geolocalization.dto.trocaEventos.PlacesResponseDTO;
import com.edu.infnet.pb.geolocalization.model.Favorito;
import com.edu.infnet.pb.geolocalization.model.PontosTrocas;
import com.edu.infnet.pb.geolocalization.repository.FavoritoRepository;
import com.edu.infnet.pb.geolocalization.repository.PontosTrocaRepository;
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
public class TrocasServices {

private final ApiServices apiServices;
private final FavoritoRepository favoritoRepository;
private final UsuarioClient usuarioClient;
private final PontosTrocaRepository pontosTrocaRepository;

    private static final Logger log =
            LogManager.getLogger(TrocasServices.class);

    public ResponseEntity<PontosTrocas> criarTroca(CreateTradeDTO createTradeDTO , String token) {
        PontosTrocas pontosTrocas = new PontosTrocas();
        PerfilResponseDTO perfilResponseDTO = usuarioClient.buscarUusarioLogado(token);

        log.info(
                "Criando ponto de troca nome={} usuarioId={}",
                createTradeDTO.nome(),
                perfilResponseDTO.id()
        );

        pontosTrocas.setNome(createTradeDTO.nome());
        pontosTrocas.setDescricao(createTradeDTO.descricao());
        pontosTrocas.setLatitude(createTradeDTO.latitude());
        pontosTrocas.setLongitude(createTradeDTO.longitude());
        pontosTrocas.setEndereco(createTradeDTO.endereco());
        pontosTrocas.setCriadorId(perfilResponseDTO.id());

        if (createTradeDTO.latitude() == null ||
                createTradeDTO.longitude() == null) {
            List<PlacesResponseDTO> locais = apiServices.buscarLocais(createTradeDTO.endereco()).block();

            if (locais != null && !locais.isEmpty()){
                PlacesResponseDTO local = locais.get(0);

                pontosTrocas.setLatitude(
                        Double.parseDouble(local.lat())
                );
                pontosTrocas.setLongitude(
                        Double.parseDouble(local.lon())
                );
            }

        }

        return ResponseEntity.ok(pontosTrocaRepository.save(pontosTrocas));
    }


    public ResponseEntity<PontosTrocas> deletandoTrocas(Long id , String token) {

        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        log.info(
                "Solicitada exclusao da troca trocaId={} usuarioId={}",
                id,
                usuario.id()
        );

        PontosTrocas pontosTrocas = pontosTrocaRepository.findById(id).orElseThrow(() -> new RuntimeException("trocas nao encontrado"));

        if (!pontosTrocas.getCriadorId().equals(usuario.id())) {
            log.warn(
                    "Usuario sem permissao usuarioId={} criadorId={} trocaId={}",
                    usuario.id(),
                    pontosTrocas.getCriadorId(),
                    id
            );

            throw new RuntimeException("Apenas o criador pode deletar a troca");
        }

        favoritoRepository.deleteByTrocaId(id);
        pontosTrocaRepository.delete(pontosTrocas);

        log.info(
                "Troca removida trocaId={} usuarioId={}",
                id,
                usuario.id()
        );

        return ResponseEntity.noContent().build();
    }


    public ResponseEntity<PontosTrocas> editarTroca(Long id, EditarEventosDTO editarEventosDTO) {

        log.info(
                "Editando troca trocaId={}",
                id
        );

        PontosTrocas pontosTrocas = pontosTrocaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento nao encontrado"));


        log.info(
                "Troca atualizada trocaId={}",
                pontosTrocas.getId()
        );
        return ResponseEntity.ok(pontosTrocaRepository.save(pontosTrocas));

    }

    public ResponseEntity<Favorito> favoritarTrocas (Long eventoId , String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        log.info(
                "Favoritando troca trocaId={} usuarioId={}",
                eventoId,
                usuario.id()
        );

        PontosTrocas pontosTrocas = pontosTrocaRepository.findById(eventoId).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));

        Favorito favorito = Favorito.builder()
                .usuarioId(usuario.id())
                .troca(pontosTrocas)
                .dataFavoritado(LocalDateTime.now())
                .build();

        favoritoRepository.save(favorito);

        log.info(
                "Troca favoritada trocaId={} usuarioId={}",
                eventoId,
                usuario.id()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(favorito);


    }


    public List<PontosTrocas> listarTrocas() {
        log.info("Listando pontos de troca");
        return pontosTrocaRepository.findAll();
    }

    public ResponseEntity<PontosTrocas> findById(Long id) {

        log.info(
                "Buscando troca por id={}",
                id
        );

        PontosTrocas pontosTrocas = pontosTrocaRepository.findById(id).orElseThrow(() -> new RuntimeException("Evento nao encontrado"));
        return ResponseEntity.ok(pontosTrocas);
    }
}


