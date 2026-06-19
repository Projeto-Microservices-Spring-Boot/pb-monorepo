package com.edu.infnet.pb.stickers.Repository;

import com.edu.infnet.pb.stickers.Entity.TradeProposal;
import com.edu.infnet.pb.stickers.Enum.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TradeProposalRepository extends JpaRepository<TradeProposal, Long> {

    List<TradeProposal> findByProposerIdOrderByCreatedAtDesc(UUID proposerId);

    List<TradeProposal> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

    List<TradeProposal> findByProposerIdAndStatus(UUID proposerId, TradeStatus status);

    List<TradeProposal> findByReceiverIdAndStatus(UUID receiverId, TradeStatus status);

    // Requisito: "impedir duplicidade de propostas iguais em aberto" =
    // mesmo par de usuários já tem QUALQUER troca PENDING entre eles,
    // independente de quem é proponente/receptor em cada uma (por isso
    // checamos as duas direções do par).
    @Query("""
        SELECT COUNT(tp) > 0 FROM TradeProposal tp
        WHERE tp.status = 'PENDING'
          AND (
                (tp.proposerId = :userA AND tp.receiverId = :userB)
             OR (tp.proposerId = :userB AND tp.receiverId = :userA)
          )
        """)
    boolean existsPendingBetween(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB
    );

    // Usado pelo job agendado de expiração: busca propostas ainda "ativas"
    // (PENDING aguardando resposta, ou ACCEPTED com encontro marcado mas
    // nunca confirmado) cujo prazo já passou.
    @Query("""
        SELECT tp FROM TradeProposal tp
        WHERE tp.status IN ('PENDING', 'ACCEPTED')
          AND tp.expiresAt < :now
        """)
    List<TradeProposal> findAllActiveExpiredBefore(@Param("now") LocalDateTime now);
}