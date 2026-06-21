package com.edu.infnet.pb.geolocalization.repository;

import com.edu.infnet.pb.geolocalization.model.Enums.StatusFigurinha;
import com.edu.infnet.pb.geolocalization.model.FigurinhaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FigurinhaUsuarioRepository extends JpaRepository<FigurinhaUsuario , Long> {
    List<FigurinhaUsuario> findByUsuarioIdAndStatus(UUID usuarioId , StatusFigurinha status);

    Optional<FigurinhaUsuario> findByUsuarioIdAndFigurinhaIdAndStatus(UUID usuarioId , Long figurinhaId , StatusFigurinha status);

    // dentro de FigurinhaUsuarioRepository

    @Query("""
    SELECT outro.usuarioId, COUNT(outro.id)
    FROM FigurinhaUsuario meuFaltante, FigurinhaUsuario outro
    WHERE meuFaltante.usuarioId = :usuarioId
    AND meuFaltante.status = 'FALTANTE'
    AND outro.status = 'REPETIDA'
    AND outro.usuarioId <> :usuarioId
    AND outro.figurinha.id = meuFaltante.figurinha.id
""")
    List<Object[]> contarOQueEleTemQueEuPreciso(@Param("usuarioId") UUID usuarioId);

    @Query("""
    SELECT outro.usuarioId, COUNT(meuRepetida.id)
    FROM FigurinhaUsuario meuRepetida, FigurinhaUsuario outro
    WHERE meuRepetida.usuarioId = :usuarioId
    AND meuRepetida.status = 'REPETIDA'
    AND outro.status = 'FALTANTE'
    AND outro.usuarioId <> :usuarioId
    AND outro.figurinha.id = meuRepetida.figurinha.id
    GROUP BY outro.usuarioId
""")
    List<Object[]> contarOQueEuTenhoQueEleNecessita(@Param("usuarioId") UUID usuarioId);


    @Query("""
SELECT outro.usuarioId,
       outro.figurinha.id,
       outro.figurinha.jogador
FROM FigurinhaUsuario meuFaltante,
     FigurinhaUsuario outro
WHERE meuFaltante.usuarioId = :usuarioId
AND meuFaltante.status = 'FALTANTE'
AND outro.status = 'REPETIDA'
AND outro.usuarioId <> :usuarioId
AND outro.figurinha.id = meuFaltante.figurinha.id
""")
    List<Object[]> buscarFigurinhasQueEleTemQueEuPreciso(UUID usuarioId);


    @Query("""
SELECT outro.usuarioId,
       meuRepetida.figurinha.id,
       meuRepetida.figurinha.jogador
FROM FigurinhaUsuario meuRepetida,
     FigurinhaUsuario outro
WHERE meuRepetida.usuarioId = :usuarioId
AND meuRepetida.status = 'REPETIDA'
AND outro.status = 'FALTANTE'
AND outro.usuarioId <> :usuarioId
AND outro.figurinha.id = meuRepetida.figurinha.id
""")
    List<Object[]> buscarFigurinhasQueEuTenhoQueEleNecessita(UUID usuarioId);





}
