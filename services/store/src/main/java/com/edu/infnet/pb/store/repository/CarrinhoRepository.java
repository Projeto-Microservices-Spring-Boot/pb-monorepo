package com.edu.infnet.pb.store.repository;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    Optional<Carrinho> findByUsuarioExternalIdAndAtivoTrue(String externalId);

    Optional<Carrinho> findByUsuarioIdAndAtivoTrue(Long userId);
}
