package com.edu.infnet.pb.stickers.Repository;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Entity.UserCollection;
import com.edu.infnet.pb.stickers.Entity.UserCollectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StickerCollectionRepository extends JpaRepository<UserCollection, UserCollectionId> {

    List<UserCollection> findByIdUserId(UUID userId);

    Optional<UserCollection> findByIdUserIdAndIdStickerId(UUID userId, Long stickerId);

    @Query("SELECT uc FROM UserCollection uc WHERE uc.id.userId = :userId AND uc.quantity > 1")
    List<UserCollection> findRepeatedByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT s FROM Sticker s
        WHERE s.id NOT IN (
            SELECT uc.id.stickerId FROM UserCollection uc
            WHERE uc.id.userId = :userId
        )
        ORDER BY s.id ASC
        """)
    List<Sticker> findMissingByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(uc) FROM UserCollection uc WHERE uc.id.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Lista TODAS as linhas de UserCollection do sistema (todos os usuários,
     * todas as stickers que cada um possui, com quantidade e nome). Usado
     * pelo TradeMatchService para calcular, em memória, todos os pares de
     * usuários que podem trocar entre si — sem partir de um usuário fixo.
     * Resultado: Object[]{ userId (UUID), stickerId (Long), quantity (Integer), playerName (String) }
     */
    @Query("SELECT uc.id.userId, uc.id.stickerId, uc.quantity, uc.sticker.name FROM UserCollection uc")
    List<Object[]> findAllCollectionsForMatching();
}

