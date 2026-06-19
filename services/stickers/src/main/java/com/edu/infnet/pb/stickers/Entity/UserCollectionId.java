package com.edu.infnet.pb.stickers.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCollectionId {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "sticker_id", nullable = false)
    private Long stickerId;
}
