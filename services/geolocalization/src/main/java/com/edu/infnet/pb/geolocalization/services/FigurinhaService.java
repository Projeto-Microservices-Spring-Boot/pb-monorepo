package com.edu.infnet.pb.geolocalization.services;

import com.edu.infnet.pb.geolocalization.client.UsuarioClient;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.FigurinhaTrocaDTO;
import com.edu.infnet.pb.geolocalization.dto.PerfilResponseDTO;
import com.edu.infnet.pb.geolocalization.dto.TrocaDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.CreateFigurinhaDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.CreateFigurinhaUsuarioDTO;
import com.edu.infnet.pb.geolocalization.dto.figurinhas.EditarQuantidadeDTO;
import com.edu.infnet.pb.geolocalization.model.Enums.StatusFigurinha;
import com.edu.infnet.pb.geolocalization.model.Enums.StatusTroca;
import com.edu.infnet.pb.geolocalization.model.Figurinha;
import com.edu.infnet.pb.geolocalization.model.FigurinhaUsuario;
import com.edu.infnet.pb.geolocalization.model.PropostaTroca;
import com.edu.infnet.pb.geolocalization.repository.FigurinhaRepository;
import com.edu.infnet.pb.geolocalization.repository.FigurinhaUsuarioRepository;
import com.edu.infnet.pb.geolocalization.repository.PropostaTrocaRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class FigurinhaService {

private final FigurinhaRepository figurinhaRepository;
private final FigurinhaUsuarioRepository figurinhaUsuarioRepository;
private final PropostaTrocaRepository propostaTrocaRepository;
private UsuarioClient usuarioClient;



public ResponseEntity<Figurinha> criarFigurinha(CreateFigurinhaDTO figurinhaDTO) {
    Figurinha figurinha = Figurinha.builder()
            .numero(figurinhaDTO.numero())
            .jogador(figurinhaDTO.jogador())
            .time(figurinhaDTO.time())
            .categoria(figurinhaDTO.categoria())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(figurinhaRepository.save(figurinha));
}

public List<Figurinha> listarFigurinhas() {
    return figurinhaRepository.findAll();
}

public ResponseEntity<FigurinhaUsuario> adicionarFigurinha(CreateFigurinhaUsuarioDTO dto , String token) {
    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

    Figurinha figurinha = figurinhaRepository.findById(dto.figurinhaId())
            .orElseThrow(() -> new RuntimeException("Figurinha nao encontrada"));

    FigurinhaUsuario figurinhaUsuario = FigurinhaUsuario.builder().usuarioId(usuario.id())
            .figurinha(figurinha)
            .status(dto.status())
            .quantidade(dto.status() == StatusFigurinha.REPETIDA ? dto.quantidade() : 1)
            .build();

    figurinhaUsuarioRepository.save(figurinhaUsuario);
    return  ResponseEntity.status(HttpStatus.CREATED).body(figurinhaUsuario);
}


public List<FigurinhaUsuario> listarMinhasFaltantes(String token) {
    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
    return figurinhaUsuarioRepository.findByUsuarioIdAndStatus(usuario.id(), StatusFigurinha.FALTANTE);
}

public List<FigurinhaUsuario> listarMinhasRepetidas(String token) {
    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
    return figurinhaUsuarioRepository.findByUsuarioIdAndStatus(usuario.id(), StatusFigurinha.REPETIDA);
}

public ResponseEntity<FigurinhaUsuario> editarQuantidade(Long id , EditarQuantidadeDTO editarQuantidadeDTO , String token) {
    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

    FigurinhaUsuario figurinhaUsuario = figurinhaUsuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Registro nao encontrado"));

    if (!figurinhaUsuario.getUsuarioId().equals(usuario.id())) {
        throw new RuntimeException("Este registro nao pertence ao usuario");
    }

    figurinhaUsuario.setQuantidade(editarQuantidadeDTO.quantidade());
    return ResponseEntity.ok(figurinhaUsuarioRepository.save(figurinhaUsuario));
}

public ResponseEntity<Void> removerFigurinhaUsuario(Long id, String token) {
    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

    FigurinhaUsuario figurinhaUsuario = figurinhaUsuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Este registro nao pertence ao usuario"));

    if (!figurinhaUsuario.getUsuarioId().equals(usuario.id())) {
        throw new RuntimeException("Este registro nao pertence ao usuario");
    }

    figurinhaUsuarioRepository.delete(figurinhaUsuario);
    return ResponseEntity.noContent().build();
}


public List<TrocaDTO> buscarMatches(String token) {

    PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);

    UUID meuId = usuario.id();

    List<Object[]> elesTemQueEuPreciso = figurinhaUsuarioRepository.contarOQueEleTemQueEuPreciso(meuId);
    List<Object[]> euTenhoQueElesPrecisam = figurinhaUsuarioRepository.contarOQueEuTenhoQueEleNecessita(meuId);
    List<Object[]> resultado = figurinhaUsuarioRepository.buscarFigurinhasQueEleTemQueEuPreciso(meuId);

    Map<UUID , Integer> mapaRecebo = new HashMap<>();
    for (Object[] linha : elesTemQueEuPreciso) {
        mapaRecebo.put((UUID) linha[0] , ((Long) linha[1]).intValue()); // aqui esta dizendo que linha[0] e o usuario e a linha[1] e a quantidade de cartas que eu preciso
    }

    Map<UUID, Integer> mapaOfereco = new HashMap<>();
    for (Object[] linha : euTenhoQueElesPrecisam) {
        mapaOfereco.put((UUID) linha[0], ((Long) linha[1]).intValue());
    }


    Map<UUID , List<FigurinhaTrocaDTO>> mapaFigurinhas = new HashMap<>();
    for (Object[] linha : resultado){
        UUID usuarioId = (UUID) linha[0];

        Long figurinhaId = (Long) linha[1];

        String jogador = (String) linha[2];

        mapaFigurinhas.computeIfAbsent(usuarioId , k -> new ArrayList<>()).add( new FigurinhaTrocaDTO(figurinhaId , jogador));
     }

return mapaRecebo.keySet().stream()
        .filter(mapaOfereco::containsKey)
        .map(outroUsuarioId -> {
            int quantoEleTem = mapaRecebo.get(outroUsuarioId);
            int quantoEuTenho = mapaOfereco.get(outroUsuarioId);
            return new TrocaDTO(outroUsuarioId , quantoEleTem + quantoEuTenho ,quantoEuTenho , quantoEleTem , mapaFigurinhas.getOrDefault(outroUsuarioId , Collections.emptyList()));
        })
        .sorted(Comparator.comparing(TrocaDTO::score).reversed())
        .toList();
}

    public List<TrocaDTO> filtrarMatchesPorUsuarios(List<TrocaDTO> matches, List<UUID> usuariosPermitidos) {
        Set<UUID> idsPermitidos = new HashSet<>(usuariosPermitidos);
        return matches.stream()
                .filter(troca -> idsPermitidos.contains(troca.usuarioId()))
                .toList();
    }


public ResponseEntity<PropostaTroca> enviarProposta(UUID destino, String token) {
        PerfilResponseDTO usuario = usuarioClient.buscarUusarioLogado(token);
        UUID origem = usuario.id();

        if (origem.equals(destino)) {
            throw new RuntimeException("Nao e possivel propor troca para si mesmo");
        }

        List<TrocaDTO> matches = buscarMatches(token);

        TrocaDTO matchEscolhido = matches.stream()
                .filter(m -> m.usuarioId().equals(destino))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Nao ha match de troca com este usuario"));

        List<Long> figurinhasQueOrigemRecebe = matchEscolhido.eleTemQueEuPreciso().stream()
                .map(FigurinhaTrocaDTO::figurinhaId)
                .toList();

        List<Object[]> resultadoOferta = figurinhaUsuarioRepository.buscarFigurinhasQueEuTenhoQueEleNecessita(origem);
        List<Long> figurinhasQueDestinoRecebe = resultadoOferta.stream()
                .filter(linha -> ((UUID) linha[0]).equals(destino))
                .map(linha -> (Long) linha[1])
                .toList();

        PropostaTroca propostaTroca = new PropostaTroca();
        propostaTroca.setUsuarioOrigem(origem);
        propostaTroca.setUsuarioDestino(destino);
        propostaTroca.setStatus(StatusTroca.PENDENTE);
        propostaTroca.setFigurinhasQueOrigemRecebe(figurinhasQueOrigemRecebe);
        propostaTroca.setFigurinhasQueDestinoRecebe(figurinhasQueDestinoRecebe);

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

        UUID origem = proposta.getUsuarioOrigem();
        UUID destino = proposta.getUsuarioDestino();

        transferirFigurinhas(origem, destino, proposta.getFigurinhasQueDestinoRecebe());
        transferirFigurinhas(destino, origem, proposta.getFigurinhasQueOrigemRecebe());

        proposta.setStatus(StatusTroca.ACEITA);
        return ResponseEntity.ok(propostaTrocaRepository.save(proposta));
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

    private void transferirFigurinhas(UUID quemDa, UUID quemRecebe, List<Long> figurinhaIds) {
        for (Long figurinhaId : figurinhaIds) {
            FigurinhaUsuario doadorRegistro = figurinhaUsuarioRepository
                    .findByUsuarioIdAndFigurinhaIdAndStatus(quemDa, figurinhaId, StatusFigurinha.REPETIDA)
                    .orElseThrow(() -> new RuntimeException("Figurinha repetida nao encontrada no doador, estoque pode ter mudado"));

            if (doadorRegistro.getQuantidade() > 1) {
                doadorRegistro.setQuantidade(doadorRegistro.getQuantidade() - 1);
                figurinhaUsuarioRepository.save(doadorRegistro);
            } else {
                figurinhaUsuarioRepository.delete(doadorRegistro);
            }

            FigurinhaUsuario faltanteDoRecebedor = figurinhaUsuarioRepository
                    .findByUsuarioIdAndFigurinhaIdAndStatus(quemRecebe, figurinhaId, StatusFigurinha.FALTANTE)
                    .orElseThrow(() -> new RuntimeException("Registro de faltante nao encontrado no recebedor"));

            figurinhaUsuarioRepository.delete(faltanteDoRecebedor);

            Figurinha figurinha = figurinhaRepository.findById(figurinhaId)
                    .orElseThrow(() -> new RuntimeException("Figurinha nao encontrada"));

            FigurinhaUsuario novaPosse = FigurinhaUsuario.builder()
                    .usuarioId(quemRecebe)
                    .figurinha(figurinha)
                    .status(StatusFigurinha.REPETIDA)
                    .quantidade(1)
                    .build();
            figurinhaUsuarioRepository.save(novaPosse);
        }
    }
}
