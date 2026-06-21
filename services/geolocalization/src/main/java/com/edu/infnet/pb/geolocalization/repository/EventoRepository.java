package com.edu.infnet.pb.geolocalization.repository;

import com.edu.infnet.pb.geolocalization.model.Eventos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoRepository extends JpaRepository<Eventos , Long> {



    @Query(value = """
    SELECT *, (
        6371 * acos(
            cos(radians(:lat)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(latitude))
        )
    ) AS distancia
    FROM pontos_mapa
    WHERE (
        6371 * acos(
            cos(radians(:lat)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(latitude))
        )
    ) <= :raioKm
    ORDER BY distancia
    """, nativeQuery = true)
    List<Eventos> buscarEventosProximos(@Param("lat") Double lat, @Param("lng") Double lng, @Param("raioKm") Double raioKm);
}
