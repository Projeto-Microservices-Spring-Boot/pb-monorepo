package com.edu.infnet.pb.store.controller.admin;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.dto.request.ProductRequest;
import com.edu.infnet.pb.store.dto.response.ProdutoResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.mapper.ProdutoMapper;
import com.edu.infnet.pb.store.repository.CategoriaRepository;
import com.edu.infnet.pb.store.repository.ProdutoRepository;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/produtos")
public class ProdutoAdminController {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoMapper produtoMapper;

    public ProdutoAdminController(ProdutoRepository produtoRepository,
                                  CategoriaRepository categoriaRepository,
                                  ProdutoMapper produtoMapper) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoMapper = produtoMapper;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody ProductRequest request) {

        validarAdmin(user);

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", request.categoriaId()));

        Produto produto = produtoMapper.toEntity(request, categoria);
        produto = produtoRepository.save(produto);

        return ResponseEntity.status(HttpStatus.CREATED).body(produtoMapper.toResponse(produto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        validarAdmin(user);

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", request.categoriaId()));

        produtoMapper.updateEntity(produto, request, categoria);
        produto = produtoRepository.save(produto);

        return ResponseEntity.ok(produtoMapper.toResponse(produto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {

        validarAdmin(user);

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

        produto.setAtivo(false);
        produtoRepository.save(produto);

        return ResponseEntity.noContent().build();
    }

    private void validarAdmin(UserPrincipal user) {
        if (user == null || !user.isAdmin()) {
            throw new BusinessException("Acesso negado. Apenas administradores podem realizar esta operação.");
        }
    }
}
