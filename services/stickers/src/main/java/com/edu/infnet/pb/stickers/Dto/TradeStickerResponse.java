package com.edu.infnet.pb.stickers.Dto;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeStickerResponse {

    private StickerResponse sticker;
    private Integer quantity;
    //Converte a entity UseCollection em dto
    public static TradeStickerResponse fromEntity(Sticker sticker, Integer quantity) {
        return TradeStickerResponse.builder()
                .sticker(StickerResponse.fromEntity(sticker))
                .quantity(quantity)
                .build();
    }
}
