package com.edu.infnet.pb.geolocalization.services;


import com.edu.infnet.pb.geolocalization.client.MatchsClient;
import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.FigurinhaDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.TradeMatchResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.kafka.PropostaAceitaEvento;
import com.edu.infnet.pb.geolocalization.event.StickerOfertaDTO;
import com.edu.infnet.pb.geolocalization.kafka.PropostaTrocaProducer;
import com.edu.infnet.pb.geolocalization.model.Enums.StatusTroca;
import com.edu.infnet.pb.geolocalization.model.PropostaTroca;
import com.edu.infnet.pb.geolocalization.repository.PropostaTrocaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class FigurinhaService {

    private final PropostaTrocaRepository propostaTrocaRepository;
    private final UsuarioClient usuarioClient;
    private final MatchsClient matchsClient;
    private final ObjectMapper objectMapper;
    private final PropostaTrocaProducer propostaTrocaProducer;

    public List<TradeMatchResponseDTO> buscarTodosOsMatches() {
        return matchsClient.buscarMatches();
    }


    public List<TradeMatchResponseDTO> buscarMeusMatches(String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
        UUID meuId = usuario.id();

        List<TradeMatchResponseDTO> todosOsMatches = matchsClient.buscarMatches();

        return todosOsMatches.stream()
                .filter(match -> match.usuarioOrigemId().equals(meuId) || match.usuarioDestinoId().equals(meuId))
                .toList();
    }

    public List<TradeMatchResponseDTO> filtrarMatchesPorUsuarios(
            List<TradeMatchResponseDTO> matches, List<UUID> usuariosPermitidos) {

        Set<UUID> idsPermitidos = new HashSet<>(usuariosPermitidos);

        return matches.stream()
                .filter(match -> idsPermitidos.contains(match.usuarioOrigemId())
                        || idsPermitidos.contains(match.usuarioDestinoId()))
                .toList();
    }



    public ResponseEntity<PropostaTroca> enviarProposta(UUID destino, String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
        UUID origem = usuario.id();

        if (origem.equals(destino)) {
            throw new RuntimeException("Nao e possivel propor troca para si mesmo");
        }

        List<TradeMatchResponseDTO> meusMatches = buscarMeusMatches(token);

        TradeMatchResponseDTO matchEscolhido = meusMatches.stream()
                .filter(m -> m.usuarioOrigemId().equals(destino) || m.usuarioDestinoId().equals(destino))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Nao ha match de troca com este usuario"));

        List<FigurinhaDTO>  stickersQueOrigemOferece;
        List<FigurinhaDTO> stickersQueDestinoOferece;

        if (matchEscolhido.usuarioOrigemId().equals(origem)) {
            // Eu sou o "A" do match → as listas já estão na ordem certa
            stickersQueOrigemOferece = matchEscolhido.stickersQueOrigemOferece();
            stickersQueDestinoOferece = matchEscolhido.stickersQueDestinoOferece();
        } else {
            // Eu sou o "B" do match → as listas estão invertidas, preciso trocar
            stickersQueOrigemOferece = matchEscolhido.stickersQueDestinoOferece();
            stickersQueDestinoOferece = matchEscolhido.stickersQueOrigemOferece();
        }

        PropostaTroca propostaTroca = new PropostaTroca();
        propostaTroca.setUsuarioOrigem(origem);
        propostaTroca.setUsuarioDestino(destino);
        propostaTroca.setStatus(StatusTroca.PENDENTE);
        propostaTroca.setStickersOrigemJson(escreverJson(stickersQueOrigemOferece));
        propostaTroca.setStickersDestinoJson(escreverJson(stickersQueDestinoOferece));


        return ResponseEntity.status(HttpStatus.CREATED).body(propostaTrocaRepository.save(propostaTroca));
    }


    public ResponseEntity<PropostaTroca> aceitarProposta(Long propostaId, String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        PropostaTroca proposta = propostaTrocaRepository.findById(propostaId)
                .orElseThrow(() -> new RuntimeException("Proposta nao encontrada"));

        if (!proposta.getUsuarioDestino().equals(usuario.id())) {
            throw new RuntimeException("Esta proposta nao pertence ao usuario");
        }

        if (proposta.getStatus() != StatusTroca.PENDENTE) {
            throw new RuntimeException("Esta proposta ja foi respondida");
        }

        proposta.setStatus(StatusTroca.ACEITA);
        PropostaTroca propostaSalva = propostaTrocaRepository.save(proposta);

        List<FigurinhaDTO> stickersOrigem = lerJson(propostaSalva.getStickersOrigemJson());
        List<FigurinhaDTO> stickersDestino = lerJson(propostaSalva.getStickersDestinoJson());

        PropostaAceitaEvento evento = new PropostaAceitaEvento(
                propostaSalva.getId(),
                propostaSalva.getUsuarioOrigem(),
                propostaSalva.getUsuarioDestino(),
                converterParaStickerOferta(stickersOrigem),
                converterParaStickerOferta(stickersDestino),
                LocalDateTime.now()
        );

        propostaTrocaProducer.enviarPropostaAceita(evento);
        return ResponseEntity.ok(propostaSalva);
    }

    public ResponseEntity<PropostaTroca> rejeitarProposta(Long propostaId, String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

        PropostaTroca proposta = propostaTrocaRepository.findById(propostaId)
                .orElseThrow(() -> new RuntimeException("Proposta nao encontrada"));

        if (!proposta.getUsuarioDestino().equals(usuario.id())) {
            throw new RuntimeException("Esta proposta nao pertence ao usuario");
        }

        if (proposta.getStatus() != StatusTroca.PENDENTE) {
            throw new RuntimeException("Esta proposta ja foi respondida");
        }

        proposta.setStatus(StatusTroca.RECUSADA);
        return ResponseEntity.ok(propostaTrocaRepository.save(proposta));
    }


    private String escreverJson(List<FigurinhaDTO> stickers) {
        try {
            return objectMapper.writeValueAsString(stickers);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao salvar dados da proposta", e);
        }
    }

    private List<FigurinhaDTO> lerJson(String json) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, FigurinhaDTO.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao ler dados da proposta", e);
        }
    }

    private List<StickerOfertaDTO> converterParaStickerOferta(List<FigurinhaDTO> stickers) {
        return stickers.stream()
                .map(s -> new StickerOfertaDTO(s.figurinhaId(), s.nomeJogador(), 1))
                .toList();
    }

}