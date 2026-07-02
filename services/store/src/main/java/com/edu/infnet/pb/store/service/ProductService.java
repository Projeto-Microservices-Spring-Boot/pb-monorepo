package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.dto.request.ProductRequest;
import com.edu.infnet.pb.store.dto.response.ProdutoResponse;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.mapper.ProdutoMapper;
import com.edu.infnet.pb.store.repository.ProdutoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;
    private final CategoryService categoryService;

    public ProductService(ProdutoRepository produtoRepository,
                          ProdutoMapper produtoMapper,
                          CategoryService categoryService) {
        this.produtoRepository = produtoRepository;
        this.produtoMapper = produtoMapper;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarAtivos(Pageable pageable) {
        return produtoRepository.findByAtivoTrue(pageable)
                .map(produtoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarTodos() {
        return produtoRepository.findAll().stream()
                .map(produtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));
        return produtoMapper.toResponse(produto);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        return produtoRepository.findByCategoriaIdAndAtivoTrue(categoriaId, pageable)
                .map(produtoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> buscarPorNome(String nome, Pageable pageable) {
        return produtoRepository.searchByNome(nome, pageable)
                .map(produtoMapper::toResponse);
    }

    @Transactional
    public ProdutoResponse criar(ProductRequest request) {
        Categoria categoria = categoryService.buscarEntidadePorId(request.categoriaId());

        Produto produto = produtoMapper.toEntity(request, categoria);
        produto = produtoRepository.save(produto);

        log.info("Produto criado: id={}, nome={}", produto.getId(), produto.getNome());
        return produtoMapper.toResponse(produto);
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProductRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

        Categoria categoria = categoryService.buscarEntidadePorId(request.categoriaId());
        produtoMapper.updateEntity(produto, request, categoria);
        produto = produtoRepository.save(produto);

        log.info("Produto atualizado: id={}", id);
        return produtoMapper.toResponse(produto);
    }

    @Transactional
    public void deletar(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));
        produtoRepository.delete(produto);
        log.info("Produto removido: id={}", id);
    }

    @Transactional
    public ProdutoResponse alterarStatus(Long id, boolean ativo) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

        produto.setAtivo(ativo);
        produto = produtoRepository.save(produto);
        log.info("Produto id={} status alterado para ativo={}", id, ativo);
        return produtoMapper.toResponse(produto);
    }

    // === Método interno ===
    @Transactional(readOnly = true)
    public Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));
    }
}
