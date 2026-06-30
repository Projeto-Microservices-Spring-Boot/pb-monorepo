package com.edu.infnet.pb.stickers.Repository;

import com.edu.infnet.pb.stickers.Entity.TradeProposal;
import com.edu.infnet.pb.stickers.Enum.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeProposalRepository extends JpaRepository<TradeProposal, Long> {

    // Todas as propostas onde o usuário é origem ou destino
    @Query("""
        SELECT tp FROM TradeProposal tp
        WHERE tp.usuarioOrigem = :userId OR tp.usuarioDestino = :userId
        ORDER BY tp.createdAt DESC
        """)
    List<TradeProposal> findByUserId(@Param("userId") UUID userId);

    // Propostas recebidas pelo usuário (ele é o destino) com status PENDENTE
    List<TradeProposal> findByUsuarioDestinoAndStatus(UUID usuarioDestino, TradeStatus status);

    // Propostas enviadas pelo usuário (ele é a origem)
    List<TradeProposal> findByUsuarioOrigemOrderByCreatedAtDesc(UUID usuarioOrigem);

    // Verifica se já existe proposta pendente entre os dois usuários
    @Query("""
        SELECT COUNT(tp) > 0 FROM TradeProposal tp
        WHERE tp.status = 'PENDENTE'
          AND (
            (tp.usuarioOrigem = :userA AND tp.usuarioDestino = :userB)
            OR
            (tp.usuarioOrigem = :userB AND tp.usuarioDestino = :userA)
          )
        """)
    boolean existsPendingBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
