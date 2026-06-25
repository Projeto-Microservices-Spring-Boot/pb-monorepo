package com.edu.infnet.pb.stickers.Dto;

import com.edu.infnet.pb.stickers.Entity.TradeItem;
import com.edu.infnet.pb.stickers.Enum.TradeSide;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeItemResponse {

    private Long stickerId;
    private String stickerCode;
    private String playerName;
    private Integer quantity;
    private TradeSide side;

    public static TradeItemResponse fromEntity(TradeItem item) {
        return TradeItemResponse.builder()
                .stickerId(item.getSticker().getId())
                .stickerCode(item.getSticker().getStickerCode())
                .playerName(item.getSticker().getName())
                .quantity(item.getQuantity())
                .side(item.getSide())
                .build();
    }
}