package com.edu.infnet.pb.stickers.Entity;

import com.edu.infnet.pb.stickers.Enum.TradeSide;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trade_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private TradeProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sticker_id", nullable = false)
    private Sticker sticker;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private TradeSide side; // ORIGEM ou DESTINO
}
