package com.edu.infnet.pb.stickers.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddStickerToCollectionRequest {

    @NotNull(message = "O id da sticker e obrigatorio")
    private Long stickerId;

    @NotNull(message = "A quantidade e obrigatoria")
    @Min(value = 1, message = "A quantidade deve ser maior que zero")
    private Integer quantity;
}
