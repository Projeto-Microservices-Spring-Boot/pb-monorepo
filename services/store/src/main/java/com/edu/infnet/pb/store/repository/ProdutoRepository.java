package com.edu.infnet.pb.store.repository;

import com.edu.infnet.pb.store.domain.produto.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrue();

    Page<Produto> findByAtivoTrue(Pageable pageable);

    List<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId);

    Page<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId, Pageable pageable);

    @Query("SELECT p FROM Produto p WHERE p.ativo = true AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    Page<Produto> searchByNome(@Param("nome") String nome, Pageable pageable);

    @Query("SELECT p FROM Produto p WHERE p.ativo = true AND p.estoque > 0")
    List<Produto> findDisponiveis();
}
