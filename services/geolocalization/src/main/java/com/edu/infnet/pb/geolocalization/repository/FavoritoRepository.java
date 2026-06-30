package com.edu.infnet.pb.geolocalization.repository;

import com.edu.infnet.pb.geolocalization.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito , Long> {

List<Favorito> findByUsuarioId(UUID usuarioId);

void deleteByEventoId(Long eventoId);
void deleteByTrocaId(Long id);

}
