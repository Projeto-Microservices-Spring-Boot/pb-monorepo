package com.edu.infnet.pb.stickers.Dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateTradeProposalDTO {
    UUID proposerId;
    UUID receiverId;
    String message;
    List<TradeItemDTO> offeredStickers;
    List<TradeItemDTO> requestedSticker;
}
