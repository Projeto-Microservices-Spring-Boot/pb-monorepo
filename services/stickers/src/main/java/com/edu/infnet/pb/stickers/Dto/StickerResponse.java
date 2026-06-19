package com.edu.infnet.pb.stickers.Dto;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StickerResponse {

    private Long id;
    private String stickerCode;
    private String name;
    private String team;
    private StickerType type;


   // Converte Sticker para StickerReponse
    public static StickerResponse fromEntity(Sticker sticker) {
        return StickerResponse.builder()
                .id(sticker.getId())
                .stickerCode(sticker.getStickerCode())
                .name(sticker.getName())
                .team(sticker.getTeam())
                .type(sticker.getType())
                .build();
    }
}
