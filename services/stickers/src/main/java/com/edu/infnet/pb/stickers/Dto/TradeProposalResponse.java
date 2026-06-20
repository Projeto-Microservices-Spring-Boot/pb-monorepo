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
    private UUID proposerId;
    private UUID receiverId;
    private TradeStatus status;
    private String message;
    private String meetingLocation;
    private LocalDateTime meetingAt;
    private boolean confirmedByProposer;
    private boolean confirmedByReceiver;
    private LocalDateTime proposerConfirmedAt;
    private LocalDateTime receiverConfirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private List<TradeStickerResponse> offeredStickers;
    private List<TradeStickerResponse> requestedStickers;

    public static TradeProposalResponse fromEntity(TradeProposal trade) {
        return TradeProposalResponse.builder()
                .id(trade.getId())
                .proposerId(trade.getProposerId())
                .receiverId(trade.getReceiverId())
                .status(trade.getStatus())
                .message(trade.getMessage())
                .meetingLocation(trade.getMeetingLocation())
                .meetingAt(trade.getMeetingAt())
                .confirmedByProposer(trade.isConfirmedByProposer())
                .confirmedByReceiver(trade.isConfirmedByReceiver())
                .proposerConfirmedAt(trade.getProposerConfirmedAt())
                .receiverConfirmedAt(trade.getReceiverConfirmedAt())
                .completedAt(trade.getCompletedAt())
                .createdAt(trade.getCreatedAt())
                .updatedAt(trade.getUpdatedAt())
                .expiresAt(trade.getExpiresAt())
                .offeredStickers(trade.getOfferedStickers().stream()
                        .map(item -> TradeStickerResponse.fromEntity(item.getSticker(), item.getQuantity()))
                        .toList())
                .requestedStickers(trade.getRequestedStickers().stream()
                        .map(item -> TradeStickerResponse.fromEntity(item.getSticker(), item.getQuantity()))
                        .toList())
                .build();
    }
}
