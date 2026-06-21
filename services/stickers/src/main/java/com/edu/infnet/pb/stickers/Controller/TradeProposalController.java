package com.edu.infnet.pb.stickers.Controller;

import com.edu.infnet.pb.stickers.Dto.AcceptTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.CreateTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.CreateTradeProposalRequest;
import com.edu.infnet.pb.stickers.Dto.TradeProposalResponse;
import com.edu.infnet.pb.stickers.Service.TradeProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

        CreateTradeProposalDTO dto = new CreateTradeProposalDTO();
        dto.setProposerId(proposerId);
        dto.setReceiverId(request.getReceiverId());
        dto.setMessage(request.getMessage());
        dto.setOfferedStickers(request.getOfferedStickers());
        dto.setRequestedStickers(request.getRequestedStickers());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                TradeProposalResponse.fromEntity(tradeProposalService.createProposal(dto)));
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeProposalResponse> findById(@PathVariable Long tradeId) {
        return ResponseEntity.ok(TradeProposalResponse.fromEntity(tradeProposalService.findById(tradeId)));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<TradeProposalResponse>> findSentByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);

        return ResponseEntity.ok(tradeProposalService.findSentBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @GetMapping("/received")
    public ResponseEntity<List<TradeProposalResponse>> findReceivedByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);

        return ResponseEntity.ok(tradeProposalService.findReceivedBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @GetMapping("/sent/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingSentByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);

        return ResponseEntity.ok(tradeProposalService.findPendingSentBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @GetMapping("/received/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingReceivedByMe(@AuthenticationPrincipal Jwt auth) {
        UUID userId = currentUserId(auth);

        return ResponseEntity.ok(tradeProposalService.findPendingReceivedBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @PatchMapping("/{tradeId}/accept")
    public ResponseEntity<TradeProposalResponse> acceptProposal(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId,
            @Valid @RequestBody AcceptTradeProposalDTO request) {

        UUID receiverId = currentUserId(auth);

        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.acceptProposal(tradeId, receiverId, request)));
    }

    @PatchMapping("/{tradeId}/reject")
    public ResponseEntity<TradeProposalResponse> rejectProposal(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId) {

        UUID receiverId = currentUserId(auth);

        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.rejectProposal(tradeId, receiverId)));
    }

    @PatchMapping("/{tradeId}/cancel")
    public ResponseEntity<TradeProposalResponse> cancelProposal(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId) {

        UUID userId = currentUserId(auth);

        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.cancelProposal(tradeId, userId)));
    }

    @PatchMapping("/{tradeId}/confirm")
    public ResponseEntity<TradeProposalResponse> confirmCompletion(
            @AuthenticationPrincipal Jwt auth,
            @PathVariable Long tradeId) {

        UUID userId = currentUserId(auth);

        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.confirmCompletion(tradeId, userId)));
    }
}