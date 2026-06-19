package com.edu.infnet.pb.stickers.Controller;

import com.edu.infnet.pb.stickers.Dto.AcceptTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.CreateTradeProposalDTO;
import com.edu.infnet.pb.stickers.Dto.TradeProposalResponse;
import com.edu.infnet.pb.stickers.Service.TradeProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeProposalController {

    private final TradeProposalService tradeProposalService;

    @PostMapping
    public ResponseEntity<TradeProposalResponse> createProposal(
            @Valid @RequestBody CreateTradeProposalDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                TradeProposalResponse.fromEntity(tradeProposalService.createProposal(request)));
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<TradeProposalResponse> findById(@PathVariable Long tradeId) {
        return ResponseEntity.ok(TradeProposalResponse.fromEntity(tradeProposalService.findById(tradeId)));
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<TradeProposalResponse>> findSentBy(@PathVariable UUID userId) {
        return ResponseEntity.ok(tradeProposalService.findSentBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @GetMapping("/received/{userId}")
    public ResponseEntity<List<TradeProposalResponse>> findReceivedBy(@PathVariable UUID userId) {
        return ResponseEntity.ok(tradeProposalService.findReceivedBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @GetMapping("/sent/{userId}/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingSentBy(@PathVariable UUID userId) {
        return ResponseEntity.ok(tradeProposalService.findPendingSentBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @GetMapping("/received/{userId}/pending")
    public ResponseEntity<List<TradeProposalResponse>> findPendingReceivedBy(@PathVariable UUID userId) {
        return ResponseEntity.ok(tradeProposalService.findPendingReceivedBy(userId).stream()
                .map(TradeProposalResponse::fromEntity)
                .toList());
    }

    @PatchMapping("/{tradeId}/accept")
    public ResponseEntity<TradeProposalResponse> acceptProposal(
            @PathVariable Long tradeId,
            @RequestParam UUID receiverId,
            @Valid @RequestBody AcceptTradeProposalDTO request) {
        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.acceptProposal(tradeId, receiverId, request)));
    }

    @PatchMapping("/{tradeId}/reject")
    public ResponseEntity<TradeProposalResponse> rejectProposal(
            @PathVariable Long tradeId,
            @RequestParam UUID receiverId) {
        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.rejectProposal(tradeId, receiverId)));
    }

    @PatchMapping("/{tradeId}/cancel")
    public ResponseEntity<TradeProposalResponse> cancelProposal(
            @PathVariable Long tradeId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.cancelProposal(tradeId, userId)));
    }

    @PatchMapping("/{tradeId}/confirm")
    public ResponseEntity<TradeProposalResponse> confirmCompletion(
            @PathVariable Long tradeId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(TradeProposalResponse.fromEntity(
                tradeProposalService.confirmCompletion(tradeId, userId)));
    }
}
