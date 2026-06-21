package com.edu.infnet.pb.geolocalization.repository;




import com.edu.infnet.pb.geolocalization.model.EventoCopa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoCopaRepository extends JpaRepository<EventoCopa, Long> {

//    @Query("SELECT e FROM EventoCopa e WHERE e.id = :id AND e.tipo = :tipo")
//    Optional<EventoCopa> findByIdAndTipo(@Param("id") Long id, @Param("tipo") PointType tipo);

}
