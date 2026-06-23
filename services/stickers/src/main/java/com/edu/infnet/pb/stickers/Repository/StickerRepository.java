package com.edu.infnet.pb.stickers.Repository;

import com.edu.infnet.pb.stickers.Entity.Sticker;
import com.edu.infnet.pb.stickers.Enum.StickerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerRepository extends JpaRepository<Sticker, Long> {

    Optional<Sticker> findByStickerCode(String stickerCode);

    List<Sticker> findByTeam(String team);

    List<Sticker> findByType(StickerType type);

    List<Sticker> findByNameContainingIgnoreCase(String name);


//Tudo Junto porém cada param é opicional
    @Query("""
        SELECT s FROM Sticker s
        WHERE (:team IS NULL OR s.team = :team)
          AND (:type IS NULL OR s.type = :type)
          AND (:stickerCode IS NULL OR s.stickerCode = :stickerCode)
          AND (:playerName IS NULL OR s.name = :playerName)
        ORDER BY s.id ASC
        """)
    List<Sticker> search(
            @Param("team")        String team,
            @Param("type")        StickerType type,
            @Param("stickerCode") String stickerCode,
            @Param("playerName") String playerName
    );

    boolean existsByStickerCode(String stickerCode);
}
