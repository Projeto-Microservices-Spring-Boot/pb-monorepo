package com.edu.infnet.pb.stickers.Controller;

import com.edu.infnet.pb.stickers.Dto.Communication.Trocas;
import com.edu.infnet.pb.stickers.Dto.TradeProposalRequest;
import com.edu.infnet.pb.stickers.Dto.TradeProposalResponse;
import com.edu.infnet.pb.stickers.Service.TradeProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trades/proposals")
@RequiredArgsConstructor
public class TradeProposalController {

    private static final Logger log = LogManager.getLogger(TradeProposalController.class);

    private final TradeProposalService proposalService;

    private UUID currentUserId(Jwt auth) {
        return UUID.fromString(auth.getSubject());
    }
    @GetMapping("/pontostroca")
    public ResponseEntity<List<Trocas>> listarTrocas() {
        return ResponseEntity.ok(proposalService.listarTrocas());
    }

    /**
     * Cria uma nova proposta de troca.
     * O usuário logado é sempre o origem.
     */
    @PostMapping
    public ResponseEntity<TradeProposalResponse> create(
            @AuthenticationPrincipal Jwt auth,
            @Valid @RequestBody TradeProposalRequest request) {
        UUID userId = currentUserId(auth);
        log.info("Usuário id={} criando proposta para usuário id={}", userId, request.getUsuarioDestino());

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                proposalService.create(userId, request));

        log.info("Proposta id={} criada com sucesso", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todas as propostas do usuário logado (enviadas e recebidas).
     */
    @GetMapping
    public ResponseEntity<List<TradeProposalResponse>> findMyProposals(
            @AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);
        log.info("Buscando propostas do usuário id={}", userId);

        var result = proposalService.findByUserId(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList();

        log.info("Encontradas {} propostas para usuário id={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Lista propostas pendentes recebidas pelo usuário logado.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingReceived(
            @AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);
        log.info("Buscando propostas pendentes recebidas pelo usuário id={}", userId);

        var result = proposalService.findPendingReceived(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList();

        log.info("Encontradas {} propostas pendentes para usuário id={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Busca uma proposta por id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TradeProposalResponse> findById(@PathVariable Long id) {
        log.info("Buscando proposta id={}", id);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                proposalService.findById(id));

        return ResponseEntity.ok(response);
    }

    /**
     * Aceita uma proposta. Só o usuário destino pode aceitar.
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<TradeProposalResponse> accept(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long id) {
        UUID userId = currentUserId(auth);
        log.info("Usuário id={} aceitando proposta id={}", userId, id);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                proposalService.accept(userId, id));

        log.info("Proposta id={} aceita com sucesso", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Recusa uma proposta. Só o usuário destino pode recusar.
     */
    @PatchMapping("/{id}/refuse")
    public ResponseEntity<TradeProposalResponse> refuse(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long id) {
        UUID userId = currentUserId(auth);
        log.info("Usuário id={} recusando proposta id={}", userId, id);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                proposalService.refuse(userId, id));

        log.info("Proposta id={} recusada com sucesso", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela uma proposta. Só o usuário origem pode cancelar.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TradeProposalResponse> cancel(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long id) {
        UUID userId = currentUserId(auth);
        log.info("Usuário id={} cancelando proposta id={}", userId, id);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                proposalService.cancel(userId, id));

        log.info("Proposta id={} cancelada com sucesso", id);
        return ResponseEntity.ok(response);
    }
}
