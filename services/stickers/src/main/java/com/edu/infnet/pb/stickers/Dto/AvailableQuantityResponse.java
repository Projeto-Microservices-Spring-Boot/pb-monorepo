package com.edu.infnet.pb.stickers.Dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvailableQuantityResponse {

    private Long stickerId;
    private int availableQuantity;
}
