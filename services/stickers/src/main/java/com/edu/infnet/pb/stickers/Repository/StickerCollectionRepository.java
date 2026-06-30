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

}
