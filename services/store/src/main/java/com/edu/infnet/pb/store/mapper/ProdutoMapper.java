package com.edu.infnet.pb.store.mapper;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.dto.request.ProductRequest;
import com.edu.infnet.pb.store.dto.response.ProdutoResponse;
import org.springframework.stereotype.Component;

@Component
public class ProdutoMapper {

    public Produto toEntity(ProductRequest request, Categoria categoria) {
        Produto produto = new Produto(
                request.nome(),
                request.descricao(),
                request.preco(),
                request.estoque(),
                categoria
        );
        produto.setImagemUrl(request.imagemUrl());
        return produto;
    }

    public ProdutoResponse toResponse(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getEstoque(),
                produto.getImagemUrl(),
                produto.getAtivo(),
                produto.getCategoria().getId(),
                produto.getCategoria().getNome(),
                produto.getCreatedAt()
        );
    }

    public void updateEntity(Produto produto, ProductRequest request, Categoria categoria) {
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setEstoque(request.estoque());
        produto.setImagemUrl(request.imagemUrl());
        produto.setCategoria(categoria);
    }
}
