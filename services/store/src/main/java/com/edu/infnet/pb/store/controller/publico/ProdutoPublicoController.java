package com.edu.infnet.pb.store.controller.publico;

import com.edu.infnet.pb.store.dto.response.ProdutoResponse;
import com.edu.infnet.pb.store.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publico/produtos")
public class ProdutoPublicoController {

    private final ProductService productService;

    public ProdutoPublicoController(ProductService productService) {
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

    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<Page<ProdutoResponse>> buscarPorCategoria(
            @PathVariable Long categoriaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.buscarPorCategoria(categoriaId, pageable));
    }

    @GetMapping("/busca")
    public ResponseEntity<Page<ProdutoResponse>> buscar(
            @RequestParam String nome,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.buscarPorNome(nome, pageable));
    }
}
