package com.edu.infnet.pb.store.controller.admin;

import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.pedido.StatusPedido;
import com.edu.infnet.pb.store.dto.response.PedidoResponse;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.kafka.event.OrderStatusChangedEvent;
import com.edu.infnet.pb.store.kafka.producer.OrderEventProducer;
import com.edu.infnet.pb.store.mapper.PedidoMapper;
import com.edu.infnet.pb.store.repository.PedidoRepository;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pedidos")
public class PedidoAdminController {

    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;
    private final OrderEventProducer orderEventProducer;

    public PedidoAdminController(PedidoRepository pedidoRepository,
                                 PedidoMapper pedidoMapper,
                                 OrderEventProducer orderEventProducer) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoMapper = pedidoMapper;
        this.orderEventProducer = orderEventProducer;
    }

    @GetMapping
    public ResponseEntity<Page<PedidoResponse>> listarTodos(
            @CurrentUser UserPrincipal user,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PedidoResponse> pedidos = pedidoRepository.findAll(pageable)
                .map(pedidoMapper::toResponse);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));
        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<PedidoResponse> atualizarStatus(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @RequestParam StatusPedido status) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        StatusPedido statusAnterior = pedido.getStatus();
        pedido.setStatus(status);
        pedido = pedidoRepository.save(pedido);

        // Publicar evento de mudança de status
        orderEventProducer.sendStatusChanged(new OrderStatusChangedEvent(
                pedido.getId(),
                pedido.getUsuario().getExternalId(),
                statusAnterior.name(),
                status.name()
        ));

        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }
}
