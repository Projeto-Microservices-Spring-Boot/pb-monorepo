package com.edu.infnet.pb.store.controller.admin;

import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.pedido.StatusPedido;
import com.edu.infnet.pb.store.dto.response.PedidoResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
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

        validarAdmin(user);

        Page<PedidoResponse> pedidos = pedidoRepository.findAll(pageable)
                .map(pedidoMapper::toResponse);

        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {

        validarAdmin(user);

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

        validarAdmin(user);

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        StatusPedido statusAnterior = pedido.getStatus();

        if (statusAnterior == status) {
            throw new BusinessException("O pedido já está com o status: " + status.name());
        }

        if (!transicaoValida(statusAnterior, status)) {
            throw new BusinessException(
                    "Transição de status inválida: " + statusAnterior.name() + " -> " + status.name()
            );
        }

        pedido.setStatus(status);
        pedido = pedidoRepository.save(pedido);

        orderEventProducer.sendStatusChanged(new OrderStatusChangedEvent(
                pedido.getId(),
                pedido.getUsuario().getExternalId(),
                statusAnterior.name(),
                status.name()
        ));

        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    private void validarAdmin(UserPrincipal user) {
        if (user == null || !user.isAdmin()) {
            throw new BusinessException("Acesso negado. Apenas administradores podem realizar esta operação.");
        }
    }

    private boolean transicaoValida(StatusPedido atual, StatusPedido novo) {
        return switch (atual) {
            case PENDENTE -> novo == StatusPedido.AGUARDANDO_PAGAMENTO
                    || novo == StatusPedido.PAGO
                    || novo == StatusPedido.CANCELADO;

            case AGUARDANDO_PAGAMENTO -> novo == StatusPedido.PAGO
                    || novo == StatusPedido.CANCELADO;

            case PAGO -> novo == StatusPedido.EM_SEPARACAO
                    || novo == StatusPedido.CANCELADO;

            case EM_SEPARACAO -> novo == StatusPedido.ENVIADO
                    || novo == StatusPedido.CANCELADO;

            case ENVIADO -> novo == StatusPedido.ENTREGUE;

            case ENTREGUE, CANCELADO -> false;
        };
    }
}
