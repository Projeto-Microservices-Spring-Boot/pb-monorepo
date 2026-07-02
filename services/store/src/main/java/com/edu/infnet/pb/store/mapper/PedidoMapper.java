package com.edu.infnet.pb.store.mapper;

import com.edu.infnet.pb.store.domain.pedido.ItemPedido;
import com.edu.infnet.pb.store.domain.pedido.Pedido;
import com.edu.infnet.pb.store.dto.response.PedidoResponse;
import com.edu.infnet.pb.store.dto.response.PedidoResponse.ItemPedidoResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PedidoMapper {

    public PedidoResponse toResponse(Pedido pedido) {
        List<ItemPedidoResponse> itens = pedido.getItens().stream()
                .map(this::toItemResponse)
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuario().getExternalId(),
                pedido.getStatus().name(),
                pedido.getValorTotal(),
                itens,
                pedido.getCreatedAt()
        );
    }

    private ItemPedidoResponse toItemResponse(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getId(),
                item.getProduto().getId(),
                item.getProduto().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }
}
