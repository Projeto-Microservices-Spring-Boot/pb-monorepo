package com.edu.infnet.pb.store.controller.admin;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import com.edu.infnet.pb.store.dto.request.CategoryRequest;
import com.edu.infnet.pb.store.dto.response.CategoriaResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.mapper.CategoriaMapper;
import com.edu.infnet.pb.store.repository.CategoriaRepository;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categorias")
public class CategoriaAdminController {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoriaAdminController(CategoriaRepository categoriaRepository,
                                    CategoriaMapper categoriaMapper) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CategoryRequest request) {

        validarAdmin(user);

        if (categoriaRepository.existsByNome(request.nome())) {
            throw new BusinessException("Já existe uma categoria com o nome informado");
        }

        Categoria categoria = categoriaMapper.toEntity(request);
        categoria = categoriaRepository.save(categoria);

        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaMapper.toResponse(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        validarAdmin(user);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        categoriaRepository.findByNome(request.nome())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new BusinessException("Já existe outra categoria com o nome informado");
                });

        categoriaMapper.updateEntity(categoria, request);
        categoria = categoriaRepository.save(categoria);

        return ResponseEntity.ok(categoriaMapper.toResponse(categoria));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {

        validarAdmin(user);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        categoria.setAtivo(false);
        categoriaRepository.save(categoria);

        return ResponseEntity.noContent().build();
    }

    private void validarAdmin(UserPrincipal user) {
        if (user == null || !user.isAdmin()) {
            throw new BusinessException("Acesso negado. Apenas administradores podem realizar esta operação.");
        }
    }
}
