package com.edu.infnet.pb.stickers.Dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlbumProgressResponse {

    private long ownedStickers;
    private double progressPercentage;
}
