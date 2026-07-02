package com.edu.infnet.pb.store.controller.publico;

import com.edu.infnet.pb.store.dto.response.CategoriaResponse;
import com.edu.infnet.pb.store.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publico/categorias")
public class CategoriaPublicoController {

    private final CategoryService categoryService;

    public CategoriaPublicoController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarAtivas() {
        return ResponseEntity.ok(categoryService.listarAtivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.buscarPorId(id));
    }
}
