package com.edu.infnet.pb.stickers.Dto;

import com.edu.infnet.pb.stickers.Entity.TradeProposal;
import com.edu.infnet.pb.stickers.Enum.TradeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TradeProposalResponse {

    private Long id;
    private UUID usuarioOrigem;
    private UUID usuarioDestino;
    private TradeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private List<TradeItemResponse> itensOrigem;
    private List<TradeItemResponse> itensDestino;

    public static TradeProposalResponse fromEntity(TradeProposal proposal) {
        return TradeProposalResponse.builder()
                .id(proposal.getId())
                .usuarioOrigem(proposal.getUsuarioOrigem())
                .usuarioDestino(proposal.getUsuarioDestino())
                .status(proposal.getStatus())
                .createdAt(proposal.getCreatedAt())
                .resolvedAt(proposal.getResolvedAt())
                .itensOrigem(proposal.getItems().stream()
                        .filter(i -> i.getSide().name().equals("ORIGEM"))
                        .map(TradeItemResponse::fromEntity)
                        .toList())
                .itensDestino(proposal.getItems().stream()
                        .filter(i -> i.getSide().name().equals("DESTINO"))
                        .map(TradeItemResponse::fromEntity)
                        .toList())
                .build();
    }
}
