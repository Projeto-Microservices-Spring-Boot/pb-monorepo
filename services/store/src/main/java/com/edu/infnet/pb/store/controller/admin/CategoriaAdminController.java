package com.edu.infnet.pb.store.controller.admin;

import com.edu.infnet.pb.store.dto.request.CategoryRequest;
import com.edu.infnet.pb.store.dto.response.CategoriaResponse;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import com.edu.infnet.pb.store.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categorias")
public class CategoriaAdminController {

    private final CategoryService categoryService;

    public CategoriaAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarTodas(@CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(categoryService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CategoryRequest request) {
        CategoriaResponse response = categoryService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {
        categoryService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CategoriaResponse> alterarStatus(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        return ResponseEntity.ok(categoryService.alterarStatus(id, ativo));
    }
}
