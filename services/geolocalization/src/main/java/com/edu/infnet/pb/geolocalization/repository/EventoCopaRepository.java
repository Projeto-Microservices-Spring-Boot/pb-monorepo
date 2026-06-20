package com.infnet.geolocalizacao.repository;



import com.infnet.geolocalizacao.model.Enums.PointType;
import com.infnet.geolocalizacao.model.EventoCopa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventoCopaRepository extends JpaRepository<EventoCopa, Long> {

    @Query("SELECT e FROM EventoCopa e WHERE e.id = :id AND e.tipo = :tipo")
    Optional<EventoCopa> findByIdAndTipo(@Param("id") Long id, @Param("tipo") PointType tipo);

}
