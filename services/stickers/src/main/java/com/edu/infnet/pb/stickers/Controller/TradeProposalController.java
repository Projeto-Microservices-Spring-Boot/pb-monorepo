package com.edu.infnet.pb.stickers.Controller;

import com.edu.infnet.pb.stickers.Dto.AcceptTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.CreateTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.CreateTradeProposalRequest;
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
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeProposalController {

    private final TradeProposalService tradeProposalService;
    private static final Logger log = LogManager.getLogger(TradeProposalController.class);

    /**
     * Extrai o userId a partir do "sub" do JWT já validado pelo Spring Security
     * (Resource Server configurado com JwtDecoder). Centralizado aqui para não
     * repetir UUID.fromString(auth.getSubject()) em cada método do controller.
     * É a ÚNICA fonte confiável de identidade: nunca aceitar userId vindo de
     * @PathVariable, @RequestParam ou do body.
     */
    private UUID currentUserId(Jwt auth) {
        return UUID.fromString(auth.getSubject());
    }

    @PostMapping
    public ResponseEntity<TradeProposalResponse> createProposal(
            @AuthenticationPrincipal Jwt auth,
            @Valid @RequestBody CreateTradeProposalRequest request) {

        UUID proposerId = currentUserId(auth);
        log.info("Criando proposta de troca: proposer id={} receiver id={}", proposerId, request.getReceiverId());
        CreateTradeProposalDTO dto = new CreateTradeProposalDTO();
        dto.setProposerId(proposerId);
        dto.setReceiverId(request.getReceiverId());
        dto.setMessage(request.getMessage());
        dto.setOfferedStickers(request.getOfferedStickers());
        dto.setRequestedStickers(request.getRequestedStickers());

        TradeProposalResponse response = TradeProposalResponse.fromEntity(tradeProposalService.createProposal(dto));
        log.info("Proposta de troca criada com sucesso id={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeProposalResponse> findById(@PathVariable Long tradeId) {
        log.info("Buscando proposta id={}", tradeId);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(tradeProposalService.findById(tradeId));

        log.info("Proposta id={} encontrada", tradeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<TradeProposalResponse>> findSentByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);
        log.info("Buscando propostas enviadas pelo usuário id={}", userId);

        var result = tradeProposalService.findSentBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList();

        log.info("Encontradas {} propostas enviadas pelo usuário id={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/received")
    public ResponseEntity<List<TradeProposalResponse>> findReceivedByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);
        log.info("Buscando propostas recebidas pelo usuário id={}", userId);

        var result = tradeProposalService.findReceivedBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList();

        log.info("Encontradas {} propostas recebidas pelo usuário id={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sent/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingSentByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);
        log.info("Buscando propostas pendentes enviadas pelo usuário id={}", userId);

        var result = tradeProposalService.findPendingSentBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList();

        log.info("Encontradas {} propostas pendentes enviadas pelo usuário id={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/received/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingReceivedByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);
        log.info("Buscando propostas pendentes recebidas pelo usuário id={}", userId);

        var result = tradeProposalService.findPendingReceivedBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList();

        log.info("Encontradas {} propostas pendentes recebidas pelo usuário id={}", result.size(), userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{tradeId}/accept")
    public ResponseEntity<TradeProposalResponse> acceptProposal(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId,
            @Valid @RequestBody AcceptTradeProposalDTO request) {

        UUID receiverId = currentUserId(auth);
        log.info("Aceitando proposta id={} pelo usuário id={}", tradeId, receiverId);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                tradeProposalService.acceptProposal(tradeId, receiverId, request));

        log.info("Proposta id={} aceita com sucesso pelo usuário id={}", tradeId, receiverId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tradeId}/reject")
    public ResponseEntity<TradeProposalResponse> rejectProposal(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId) {

        UUID receiverId = currentUserId(auth);
        log.info("Recusando proposta id={} pelo usuário id={}", tradeId, receiverId);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                tradeProposalService.rejectProposal(tradeId, receiverId));

        log.info("Proposta id={} recusada com sucesso pelo usuário id={}", tradeId, receiverId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tradeId}/cancel")
    public ResponseEntity<TradeProposalResponse> cancelProposal(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId) {

        UUID userId = currentUserId(auth);
        log.info("Cancelando proposta id={} pelo usuário id={}", tradeId, userId);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                tradeProposalService.cancelProposal(tradeId, userId));

        log.info("Proposta id={} cancelada com sucesso pelo usuário id={}", tradeId, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{tradeId}/confirm")
    public ResponseEntity<TradeProposalResponse> confirmCompletion(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId) {

        UUID userId = currentUserId(auth);
        log.info("Confirmando conclusão da proposta id={} pelo usuário id={}", tradeId, userId);

        TradeProposalResponse response = TradeProposalResponse.fromEntity(
                tradeProposalService.confirmCompletion(tradeId, userId));

        log.info("Proposta id={} confirmada com sucesso pelo usuário id={}", tradeId, userId);
        return ResponseEntity.ok(response);
    }
}