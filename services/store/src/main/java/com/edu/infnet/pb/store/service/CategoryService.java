package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.domain.produto.Categoria;
import com.edu.infnet.pb.store.dto.request.CategoryRequest;
import com.edu.infnet.pb.store.dto.response.CategoriaResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.mapper.CategoriaMapper;
import com.edu.infnet.pb.store.repository.CategoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoryService(CategoriaRepository categoriaRepository, CategoriaMapper categoriaMapper) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByAtivoTrue().stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));
        return categoriaMapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse criar(CategoryRequest request) {
        if (categoriaRepository.existsByNome(request.nome())) {
            throw new BusinessException("Já existe uma categoria com o nome: " + request.nome());
        }

        Categoria categoria = categoriaMapper.toEntity(request);
        categoria = categoriaRepository.save(categoria);
        log.info("Categoria criada: id={}, nome={}", categoria.getId(), categoria.getNome());
        return categoriaMapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoryRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        // Verifica se o novo nome já existe (caso tenha mudado)
        categoriaRepository.findByNome(request.nome())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new BusinessException("Já existe uma categoria com o nome: " + request.nome());
                });

        categoriaMapper.updateEntity(categoria, request);
        categoria = categoriaRepository.save(categoria);
        log.info("Categoria atualizada: id={}", id);
        return categoriaMapper.toResponse(categoria);
    }

    @Transactional
    public void deletar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        if (!categoria.getProdutos().isEmpty()) {
            throw new BusinessException("Não é possível excluir categoria com produtos vinculados. Desative-a.");
        }

        categoriaRepository.delete(categoria);
        log.info("Categoria removida: id={}", id);
    }

    @Transactional
    public CategoriaResponse alterarStatus(Long id, boolean ativo) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        categoria.setAtivo(ativo);
        categoria = categoriaRepository.save(categoria);
        log.info("Categoria id={} status alterado para ativo={}", id, ativo);
        return categoriaMapper.toResponse(categoria);
    }

    // === Método interno usado pelo ProductService ===
    @Transactional(readOnly = true)
    public Categoria buscarEntidadePorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));
    }
}
