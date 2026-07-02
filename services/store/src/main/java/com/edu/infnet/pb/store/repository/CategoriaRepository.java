package com.edu.infnet.pb.store.repository;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNome(String nome);

    List<Categoria> findByAtivoTrue();

    boolean existsByNome(String nome);
}
