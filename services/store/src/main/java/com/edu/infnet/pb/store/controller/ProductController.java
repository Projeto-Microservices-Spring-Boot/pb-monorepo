package com.edu.infnet.pb.store.controller;

import com.edu.infnet.pb.store.dto.response.ProdutoResponse;
import com.edu.infnet.pb.store.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller legado mantido para compatibilidade.
 * Novos endpoints devem usar /api/publico/produtos ou /api/admin/produtos.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.listarAtivos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productService.buscarPorId(id));
    }
}
