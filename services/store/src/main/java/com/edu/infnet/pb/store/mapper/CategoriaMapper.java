package com.edu.infnet.pb.store.mapper;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import com.edu.infnet.pb.store.dto.request.CategoryRequest;
import com.edu.infnet.pb.store.dto.response.CategoriaResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria toEntity(CategoryRequest request) {
        return new Categoria(request.nome(), request.descricao());
    }

    public CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getAtivo(),
                categoria.getProdutos() != null ? categoria.getProdutos().size() : 0,
                categoria.getCreatedAt()
        );
    }

    public void updateEntity(Categoria categoria, CategoryRequest request) {
        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
    }
}
