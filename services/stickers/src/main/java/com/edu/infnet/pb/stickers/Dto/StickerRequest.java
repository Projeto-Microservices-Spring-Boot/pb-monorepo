package com.edu.infnet.pb.stickers.Dto;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StickerRequest {


    private String stickerCode;

    private String name;

    private String team;

    private StickerType type;


    public Sticker toEntity() {
        return Sticker.builder()
                .stickerCode(stickerCode)
                .name(name)
                .team(team)
                .type(type)
                .build();
    }
}
