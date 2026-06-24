package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import com.edu.infnet.pb.store.domain.pedido.ItemPedido;
import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.domain.pedido.StatusPedido;
import com.edu.infnet.pb.store.dto.event.PedidoCriadoEvent;
import com.edu.infnet.pb.store.dto.response.CheckoutResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.repository.CarrinhoRepository;
import com.edu.infnet.pb.store.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CarrinhoRepository carrinhoRepository;
    private final PedidoRepository pedidoRepository;
    private final CheckoutEventPublisher checkoutEventPublisher;

    public CheckoutService(CarrinhoRepository carrinhoRepository,
                           PedidoRepository pedidoRepository,
                           CheckoutEventPublisher checkoutEventPublisher) {
        this.carrinhoRepository = carrinhoRepository;
        this.pedidoRepository = pedidoRepository;
        this.checkoutEventPublisher = checkoutEventPublisher;
    }

    @Transactional
    public CheckoutResponse finalizarCompra(Long userId) {
        Carrinho carrinho = carrinhoRepository.findByUsuarioIdAndAtivoTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho", "userId", userId));

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
                throw new BusinessException("Quantidade inválida para o produto " + itemCarrinho.getProduto().getNome());
            }

            itemCarrinho.getProduto().reduzirEstoque(itemCarrinho.getQuantidade());

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setProduto(itemCarrinho.getProduto());
            itemPedido.setQuantidade(itemCarrinho.getQuantidade());
            itemPedido.setPrecoUnitario(itemCarrinho.getPrecoUnitario());

            pedido.adicionarItem(itemPedido);
        }

        pedido.recalcularTotal();

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        carrinho.limpar();
        carrinho.setAtivo(false);

        PedidoCriadoEvent event = new PedidoCriadoEvent(
                pedidoSalvo.getId(),
                userId,
                pedidoSalvo.getValorTotal()
        );

        checkoutEventPublisher.publicarPedidoCriado(event);

        log.info("Checkout finalizado. pedidoId={}, userId={}, total={}, status={}",
                pedidoSalvo.getId(),
                userId,
                pedidoSalvo.getValorTotal(),
                pedidoSalvo.getStatus());

        return new CheckoutResponse(
                pedidoSalvo.getId(),
                userId,
                pedidoSalvo.getValorTotal(),
                pedidoSalvo.getStatus().name(),
                "Pedido criado com sucesso e enviado para processamento de pagamento"
        );
    }
}
