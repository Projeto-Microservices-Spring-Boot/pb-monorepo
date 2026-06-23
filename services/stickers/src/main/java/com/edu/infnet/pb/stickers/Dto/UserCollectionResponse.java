package com.edu.infnet.pb.stickers.Dto;

import com.edu.infnet.pb.stickers.Entity.UserCollection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserCollectionResponse {

    private UUID userId;
    private StickerResponse sticker;
    private Integer quantity;
    private int repeatedCount;
    private LocalDateTime addedAt;


    //Converte a entity UseCollection em dto
    public static UserCollectionResponse fromEntity(UserCollection collection) {
        return UserCollectionResponse.builder()
                .userId(collection.getId().getUserId())
                .sticker(StickerResponse.fromEntity(collection.getSticker()))
                .quantity(collection.getQuantity())
                .repeatedCount(collection.getRepeatedCount())
                .addedAt(collection.getAddedAt())
                .build();
    }
}
