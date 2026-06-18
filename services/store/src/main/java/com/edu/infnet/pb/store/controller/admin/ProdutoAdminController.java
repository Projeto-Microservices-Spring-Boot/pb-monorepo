package com.edu.infnet.pb.store.controller.admin;

import com.edu.infnet.pb.store.dto.request.ProductRequest;
import com.edu.infnet.pb.store.dto.response.ProdutoResponse;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import com.edu.infnet.pb.store.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/produtos")
public class ProdutoAdminController {

    private final ProductService productService;

    public ProdutoAdminController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listarTodos(@CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(productService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponse> atualizar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id) {
        productService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProdutoResponse> alterarStatus(
            @CurrentUser UserPrincipal user,
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        return ResponseEntity.ok(productService.alterarStatus(id, ativo));
    }
}
