package com.edu.infnet.pb.store.controller.autenticado;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import com.edu.infnet.pb.store.domain.pedido.ItemPedido;
import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.pedido.StatusPedido;
import com.edu.infnet.pb.store.dto.response.PedidoResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.kafka.event.OrderCancelledEvent;
import com.edu.infnet.pb.store.kafka.event.OrderCreatedEvent;
import com.edu.infnet.pb.store.kafka.event.OrderStatusChangedEvent;
import com.edu.infnet.pb.store.kafka.producer.OrderEventProducer;
import com.edu.infnet.pb.store.mapper.PedidoMapper;
import com.edu.infnet.pb.store.repository.CarrinhoRepository;
import com.edu.infnet.pb.store.repository.PedidoRepository;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final CarrinhoRepository carrinhoRepository;
    private final PedidoMapper pedidoMapper;
    private final OrderEventProducer orderEventProducer;

    public PedidoController(PedidoRepository pedidoRepository,
                            CarrinhoRepository carrinhoRepository,
                            PedidoMapper pedidoMapper,
                            OrderEventProducer orderEventProducer) {
        this.pedidoRepository = pedidoRepository;
        this.carrinhoRepository = carrinhoRepository;
        this.pedidoMapper = pedidoMapper;
        this.orderEventProducer = orderEventProducer;
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarMeusPedidos(@CurrentUser UserPrincipal user) {
        List<PedidoResponse> pedidos = pedidoRepository.findByUsuarioExternalId(user.externalId())
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();

        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarMeuPedido(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        validarDonoDoPedido(user, pedido);

        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<PedidoResponse> checkout(@CurrentUser UserPrincipal user) {
        Carrinho carrinho = carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue(user.externalId())
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho ativo", "usuarioExternalId", user.externalId()));

        if (carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
            throw new BusinessException("O carrinho está vazio");
        }

        Pedido pedido = new Pedido(carrinho.getUsuario());
        pedido.setStatus(StatusPedido.PENDENTE);

        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            if (itemCarrinho.getProduto() == null) {
                throw new BusinessException("Item do carrinho sem produto associado");
            }

            if (itemCarrinho.getQuantidade() == null || itemCarrinho.getQuantidade() <= 0) {
                throw new BusinessException(
                        "Quantidade inválida para o produto " + itemCarrinho.getProduto().getNome()
                );
            }

            itemCarrinho.getProduto().reduzirEstoque(itemCarrinho.getQuantidade());

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(itemCarrinho.getProduto());
            itemPedido.setQuantidade(itemCarrinho.getQuantidade());
            itemPedido.setPrecoUnitario(itemCarrinho.getPrecoUnitario());

            pedido.adicionarItem(itemPedido);
        }

        pedido.recalcularTotal();
        pedido = pedidoRepository.save(pedido);

        carrinho.limpar();
        carrinho.setAtivo(false);
        carrinhoRepository.save(carrinho);

        orderEventProducer.sendOrderCreated(new OrderCreatedEvent(
                pedido.getId(),
                pedido.getUsuario().getExternalId(),
                pedido.getValorTotal(),
                pedido.getItens().stream().mapToInt(ItemPedido::getQuantidade).sum()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoMapper.toResponse(pedido));
    }

    @PatchMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<PedidoResponse> cancelar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        validarDonoDoPedido(user, pedido);

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new BusinessException("Pedido já está cancelado");
        }

        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new BusinessException("Não é possível cancelar um pedido já entregue");
        }

        StatusPedido statusAnterior = pedido.getStatus();

        pedido.cancelar();

        pedido.getItens().forEach(item ->
                item.getProduto().adicionarEstoque(item.getQuantidade())
        );

        pedido = pedidoRepository.save(pedido);

        orderEventProducer.sendOrderCancelled(new OrderCancelledEvent(
                pedido.getId(),
                pedido.getUsuario().getExternalId(),
                "Cancelado pelo usuário"
        ));

        orderEventProducer.sendStatusChanged(new OrderStatusChangedEvent(
                pedido.getId(),
                pedido.getUsuario().getExternalId(),
                statusAnterior.name(),
                pedido.getStatus().name()
        ));

        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    private void validarDonoDoPedido(UserPrincipal user, Pedido pedido) {
        if (!pedido.getUsuario().getExternalId().equals(user.externalId())) {
            throw new BusinessException("Você não tem permissão para acessar este pedido");
        }
    }
}
