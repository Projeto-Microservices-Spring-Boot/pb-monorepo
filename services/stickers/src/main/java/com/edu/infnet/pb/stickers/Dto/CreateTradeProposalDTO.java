package com.edu.infnet.pb.stickers.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateTradeProposalDTO {
    private UUID proposerId;
    private UUID receiverId;
    private String message;
    private List<TradeItemDTO> offeredStickers;
    private List<TradeItemDTO> requestedStickers;
}
