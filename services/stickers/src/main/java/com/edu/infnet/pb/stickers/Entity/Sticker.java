package com.edu.infnet.pb.stickers.Entity;


import com.edu.infnet.pb.stickers.Enum.StickerType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stickers", indexes = {
        @Index(name = "idx_stickers_code",    columnList = "sticker_code"), // Index para facilitar a busca
        @Index(name = "idx_stickers_team", columnList = "team")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sticker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sticker_code", nullable = false, unique = true, length = 20)
    private String stickerCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "team", nullable = false,length = 100)
    private String team;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private StickerType type;




}
