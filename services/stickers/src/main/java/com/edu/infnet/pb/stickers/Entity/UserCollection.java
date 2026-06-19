package com.edu.infnet.pb.stickers.Entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_collections", indexes = {
        @Index(name = "idx_uc_user_id", columnList = "user_id") // Index Para Facilitar a busca por usuario
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCollection {

    @EmbeddedId
    private UserCollectionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stickerId")
    @JoinColumn(name = "sticker_id")
    private Sticker sticker;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public boolean hasRepeated() {
        return quantity > 1;
    }

    public int getRepeatedCount() {
        return Math.max(0, quantity - 1);
    }

}
