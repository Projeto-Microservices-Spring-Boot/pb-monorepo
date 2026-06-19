package com.edu.infnet.pb.stickers.Repository;

import com.edu.infnet.pb.stickers.Entity.TradeOfferedSticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TradeOfferedStickerRepository extends JpaRepository<TradeOfferedSticker, Long> {

    // Soma tudo que está bloqueado em trocas ainda "ativas" (PENDING ou
    // ACCEPTED) para aquela figurinha. ACCEPTED entra aqui porque, no fluxo
    // presencial, aceitar só marca o encontro — a figurinha continua com o
    // proponente até a troca ser confirmada como COMPLETED.
    @Query("""
        SELECT COALESCE(SUM(tos.quantity), 0) FROM TradeOfferedSticker tos
        WHERE tos.tradeProposal.proposerId = :userId
          AND tos.sticker.id = :stickerId
          AND tos.tradeProposal.status IN ('PENDING', 'ACCEPTED')
        """)
    int sumQuantityInActiveTrades(
            @Param("userId")    UUID userId,
            @Param("stickerId") Long stickerId
    );
}