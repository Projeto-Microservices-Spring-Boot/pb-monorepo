package com.edu.infnet.pb.stickers.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddStickerToCollectionRequest {

  
    private Long stickerId;


    private Integer quantity;
}
