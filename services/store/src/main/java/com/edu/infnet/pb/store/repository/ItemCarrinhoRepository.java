package com.edu.infnet.pb.store.repository;

import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {
}
