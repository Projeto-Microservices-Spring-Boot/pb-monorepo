package com.edu.infnet.pb.store.controller.autenticado;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.domain.usuario.UserReference;
import com.edu.infnet.pb.store.dto.response.CarrinhoResponse;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.mapper.CarrinhoMapper;
import com.edu.infnet.pb.store.repository.CarrinhoRepository;
import com.edu.infnet.pb.store.repository.UserReferenceRepository;
import com.edu.infnet.pb.store.security.CurrentUser;
import com.edu.infnet.pb.store.security.UserPrincipal;
import com.edu.infnet.pb.store.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrinho")
public class CarrinhoController {

    private final CarrinhoRepository carrinhoRepository;
    private final UserReferenceRepository userReferenceRepository;
    private final ProductService productService;
    private final CarrinhoMapper carrinhoMapper;

    public CarrinhoController(CarrinhoRepository carrinhoRepository,
                              UserReferenceRepository userReferenceRepository,
                              ProductService productService,
                              CarrinhoMapper carrinhoMapper) {
        this.carrinhoRepository = carrinhoRepository;
        this.userReferenceRepository = userReferenceRepository;
        this.productService = productService;
        this.carrinhoMapper = carrinhoMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<CarrinhoResponse> meuCarrinho(@CurrentUser UserPrincipal user) {
        Carrinho carrinho = getOrCreateCarrinho(user);
        return ResponseEntity.ok(carrinhoMapper.toResponse(carrinho));
    }

    @PostMapping("/itens")
    @Transactional
    public ResponseEntity<CarrinhoResponse> adicionarItem(
            @CurrentUser UserPrincipal user,
            @RequestParam Long produtoId,
            @RequestParam(defaultValue = "1") Integer quantidade) {

        Carrinho carrinho = getOrCreateCarrinho(user);
        Produto produto = productService.buscarEntidadePorId(produtoId);

        ItemCarrinho item = new ItemCarrinho(produto, quantidade);
        carrinho.adicionarItem(item);
        carrinho = carrinhoRepository.save(carrinho);

        return ResponseEntity.ok(carrinhoMapper.toResponse(carrinho));
    }

    @DeleteMapping("/itens/{produtoId}")
    @Transactional
    public ResponseEntity<CarrinhoResponse> removerItem(
            @CurrentUser UserPrincipal user,
            @PathVariable Long produtoId) {

        Carrinho carrinho = getOrCreateCarrinho(user);
        carrinho.removerItem(produtoId);
        carrinho = carrinhoRepository.save(carrinho);

        return ResponseEntity.ok(carrinhoMapper.toResponse(carrinho));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> limparCarrinho(@CurrentUser UserPrincipal user) {
        Carrinho carrinho = getOrCreateCarrinho(user);
        carrinho.limpar();
        carrinhoRepository.save(carrinho);
        return ResponseEntity.noContent().build();
    }

    // === Auxiliar ===

    private Carrinho getOrCreateCarrinho(UserPrincipal user) {
        return carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue(user.externalId())
                .orElseGet(() -> {
                    UserReference userRef = userReferenceRepository.findByExternalId(user.externalId())
                            .orElseGet(() -> {
                                UserReference newUser = new UserReference(user.externalId(), user.nome(), user.email());
                                return userReferenceRepository.save(newUser);
                            });
                    Carrinho novoCarrinho = new Carrinho(userRef);
                    return carrinhoRepository.save(novoCarrinho);
                });
    }
}
