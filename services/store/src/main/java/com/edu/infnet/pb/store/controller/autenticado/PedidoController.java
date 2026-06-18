package com.edu.infnet.pb.store.controller.autenticado;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.pedido.ItemPedido;
import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.usuario.UserReference;
import com.edu.infnet.pb.store.dto.response.PedidoResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.kafka.event.OrderCreatedEvent;
import com.edu.infnet.pb.store.kafka.producer.OrderEventProducer;
import com.edu.infnet.pb.store.mapper.PedidoMapper;
import com.edu.infnet.pb.store.repository.CarrinhoRepository;
import com.edu.infnet.pb.store.repository.PedidoRepository;
import com.edu.infnet.pb.store.repository.UserReferenceRepository;
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
    private final UserReferenceRepository userReferenceRepository;
    private final PedidoMapper pedidoMapper;
    private final OrderEventProducer orderEventProducer;

    public PedidoController(PedidoRepository pedidoRepository,
                            CarrinhoRepository carrinhoRepository,
                            UserReferenceRepository userReferenceRepository,
                            PedidoMapper pedidoMapper,
                            OrderEventProducer orderEventProducer) {
        this.pedidoRepository = pedidoRepository;
        this.carrinhoRepository = carrinhoRepository;
        this.userReferenceRepository = userReferenceRepository;
        this.pedidoMapper = pedidoMapper;
        this.orderEventProducer = orderEventProducer;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PedidoResponse>> meusPedidos(@CurrentUser UserPrincipal user) {
        List<PedidoResponse> pedidos = pedidoRepository
                .findByUsuarioExternalId(user.externalId())
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<PedidoResponse> buscarPorId(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        // Garantir que o usuário só veja seus próprios pedidos
        if (!pedido.getUsuario().getExternalId().equals(user.externalId())) {
            throw new BusinessException("Você não tem permissão para ver este pedido");
        }

        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<PedidoResponse> checkout(@CurrentUser UserPrincipal user) {
        // Buscar carrinho ativo
        Carrinho carrinho = carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue(user.externalId())
                .orElseThrow(() -> new BusinessException("Carrinho vazio ou não encontrado"));

        if (carrinho.getItens().isEmpty()) {
            throw new BusinessException("Carrinho está vazio. Adicione produtos antes de fazer checkout.");
        }

        // Buscar referência do usuário
        UserReference userRef = userReferenceRepository.findByExternalId(user.externalId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "externalId", user.externalId()));

        // Criar pedido a partir do carrinho
        Pedido pedido = new Pedido(userRef);
        carrinho.getItens().forEach(itemCarrinho -> {
            // Reduzir estoque
            itemCarrinho.getProduto().reduzirEstoque(itemCarrinho.getQuantidade());

            ItemPedido itemPedido = new ItemPedido(
                    itemCarrinho.getProduto(),
                    itemCarrinho.getQuantidade()
            );
            pedido.adicionarItem(itemPedido);
        });

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // Limpar carrinho após checkout
        carrinho.limpar();
        carrinhoRepository.save(carrinho);

        // Publicar evento de pedido criado
        orderEventProducer.sendOrderCreated(new OrderCreatedEvent(
                pedidoSalvo.getId(),
                userRef.getExternalId(),
                pedidoSalvo.getValorTotal(),
                pedidoSalvo.getItens().size()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoMapper.toResponse(pedidoSalvo));
    }

    @PostMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<PedidoResponse> cancelar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", id));

        if (!pedido.getUsuario().getExternalId().equals(user.externalId())) {
            throw new BusinessException("Você não tem permissão para cancelar este pedido");
        }

        // Devolver estoque
        pedido.getItens().forEach(item -> {
            item.getProduto().adicionarEstoque(item.getQuantidade());
        });

        pedido.cancelar();
        pedido = pedidoRepository.save(pedido);

        return ResponseEntity.ok(pedidoMapper.toResponse(pedido));
    }
}
