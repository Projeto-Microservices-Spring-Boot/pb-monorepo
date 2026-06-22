package com.edu.infnet.pb.stickers.Dto.Communication;



import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StickerTradeItem {

    private Long stickerId;
    private String playerName;
}
