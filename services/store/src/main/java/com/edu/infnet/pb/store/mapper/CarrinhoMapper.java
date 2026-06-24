package com.edu.infnet.pb.store.mapper;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import com.edu.infnet.pb.store.dto.response.CarrinhoResponse;
import com.edu.infnet.pb.store.dto.response.ItemCarrinhoResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CarrinhoMapper {

    public CarrinhoResponse toResponse(Carrinho carrinho) {
        List<ItemCarrinhoResponse> itens = carrinho.getItens().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = itens.stream()
                .map(ItemCarrinhoResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarrinhoResponse(
                carrinho.getId(),
                carrinho.getUsuario().getExternalId(),
                itens,
                total,
                itens.size()
        );
    }

    private ItemCarrinhoResponse toItemResponse(ItemCarrinho item) {
        return new ItemCarrinhoResponse(
                item.getId(),
                item.getProduto().getId(),
                item.getProduto().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }
}
