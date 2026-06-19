package com.edu.infnet.pb.stickers.Repository;

import com.edu.infnet.pb.stickers.Entity.TradeRequestedSticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TradeRequestedStickerRepository extends JpaRepository<TradeRequestedSticker, Long> {

    // Simétrico ao TradeOfferedStickerRepository: soma a quantidade de uma
    // figurinha do RECEPTOR que está travada em trocas ativas (PENDING ou
    // ACCEPTED), pois a proposta pede essa figurinha do receiverId.
    @Query("""
        SELECT COALESCE(SUM(trs.quantity), 0) FROM TradeRequestedSticker trs
        WHERE trs.tradeProposal.receiverId = :userId
          AND trs.sticker.id = :stickerId
          AND trs.tradeProposal.status IN ('PENDING', 'ACCEPTED')
        """)
    int sumQuantityInActiveTrades(
            @Param("userId")    UUID userId,
            @Param("stickerId") Long stickerId
    );
}