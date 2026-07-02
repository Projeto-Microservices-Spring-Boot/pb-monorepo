package com.edu.infnet.pb.store.repository;

import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.pedido.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioExternalId(String externalId);

    Page<Pedido> findByUsuarioExternalId(String externalId, Pageable pageable);

    List<Pedido> findByStatus(StatusPedido status);

    Page<Pedido> findAll(Pageable pageable);
}
