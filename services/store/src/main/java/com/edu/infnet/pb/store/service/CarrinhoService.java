package com.edu.infnet.pb.store.service;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.domain.usuario.UserReference;
import com.edu.infnet.pb.store.dto.request.AdicionarItemCarrinhoRequest;
import com.edu.infnet.pb.store.dto.request.CriarCarrinhoRequest;
import com.edu.infnet.pb.store.dto.response.CarrinhoResponse;
import com.edu.infnet.pb.store.dto.response.ItemCarrinhoResponse;
import com.edu.infnet.pb.store.exception.BusinessException;
import com.edu.infnet.pb.store.exception.ResourceNotFoundException;
import com.edu.infnet.pb.store.repository.CarrinhoRepository;
import com.edu.infnet.pb.store.repository.ProdutoRepository;
import com.edu.infnet.pb.store.repository.UserReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final ProdutoRepository produtoRepository;
    private final UserReferenceRepository userReferenceRepository;

    public CarrinhoService(CarrinhoRepository carrinhoRepository,
                           ProdutoRepository produtoRepository,
                           UserReferenceRepository userReferenceRepository) {
        this.carrinhoRepository = carrinhoRepository;
        this.produtoRepository = produtoRepository;
        this.userReferenceRepository = userReferenceRepository;
    }

    @Transactional
    public CarrinhoResponse criarCarrinho(CriarCarrinhoRequest request) {
        carrinhoRepository.findByUsuarioIdAndAtivoTrue(request.userId())
                .ifPresent(c -> {
                    throw new BusinessException("Já existe um carrinho ativo para esse usuário");
                });

        UserReference usuario = userReferenceRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", request.userId()));

        Carrinho carrinho = new Carrinho(usuario);
        Carrinho salvo = carrinhoRepository.save(carrinho);

        return toResponse(salvo);
    }

    @Transactional
    public CarrinhoResponse adicionarItem(Long carrinhoId, AdicionarItemCarrinhoRequest request) {
        Carrinho carrinho = carrinhoRepository.findById(carrinhoId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho", "id", carrinhoId));

        if (!Boolean.TRUE.equals(carrinho.getAtivo())) {
            throw new BusinessException("Carrinho inativo");
        }

        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", request.produtoId()));

        ItemCarrinho itemExistente = carrinho.getItens()
                .stream()
                .filter(item -> item.getProduto().getId().equals(produto.getId()))
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            itemExistente.setQuantidade(itemExistente.getQuantidade() + request.quantidade());
        } else {
            ItemCarrinho novoItem = new ItemCarrinho();
            novoItem.setCarrinho(carrinho);
            novoItem.setProduto(produto);
            novoItem.setQuantidade(request.quantidade());
            novoItem.setPrecoUnitario(produto.getPreco());
            carrinho.getItens().add(novoItem);
        }

        Carrinho atualizado = carrinhoRepository.save(carrinho);
        return toResponse(atualizado);
    }

    @Transactional(readOnly = true)
    public CarrinhoResponse buscarPorId(Long carrinhoId) {
        Carrinho carrinho = carrinhoRepository.findById(carrinhoId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho", "id", carrinhoId));

        return toResponse(carrinho);
    }

    @Transactional(readOnly = true)
    public CarrinhoResponse buscarCarrinhoAtivoPorUsuario(Long userId) {
        Carrinho carrinho = carrinhoRepository.findByUsuarioIdAndAtivoTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrinho ativo", "userId", userId));

        return toResponse(carrinho);
    }

    private CarrinhoResponse toResponse(Carrinho carrinho) {
        List<ItemCarrinhoResponse> itensResponse = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;
        int quantidadeItens = 0;

        if (carrinho.getItens() != null) {
            for (ItemCarrinho item : carrinho.getItens()) {
                BigDecimal subtotal = item.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade()));

                itensResponse.add(new ItemCarrinhoResponse(
                        item.getId(),
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        subtotal
                ));

                total = total.add(subtotal);
                quantidadeItens += item.getQuantidade();
            }
        }

        return new CarrinhoResponse(
                carrinho.getId(),
                carrinho.getUsuario().getExternalId(),
                itensResponse,
                total,
                quantidadeItens
        );
    }
}
