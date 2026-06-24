package com.edu.infnet.pb.store.controller.autenticado;

import com.edu.infnet.pb.store.domain.carrinho.Carrinho;
import com.edu.infnet.pb.store.domain.carrinho.ItemCarrinho;
import com.edu.infnet.pb.store.domain.produto.Produto;
import com.edu.infnet.pb.store.domain.usuario.UserReference;
import com.edu.infnet.pb.store.dto.response.CarrinhoResponse;
import com.edu.infnet.pb.store.mapper.CarrinhoMapper;
import com.edu.infnet.pb.store.repository.CarrinhoRepository;
import com.edu.infnet.pb.store.repository.UserReferenceRepository;
import com.edu.infnet.pb.store.security.UserPrincipal;
import com.edu.infnet.pb.store.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrinhoControllerTest {

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @Mock
    private UserReferenceRepository userReferenceRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CarrinhoMapper carrinhoMapper;

    @InjectMocks
    private CarrinhoController carrinhoController;

    @Test
    void deveRetornarCarrinhoExistente() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho carrinho = new Carrinho(userRef);
        CarrinhoResponse response = mock(CarrinhoResponse.class);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.of(carrinho));
        when(carrinhoMapper.toResponse(carrinho)).thenReturn(response);

        ResponseEntity<CarrinhoResponse> result = carrinhoController.meuCarrinho(user);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(carrinhoMapper).toResponse(carrinho);
        verifyNoInteractions(userReferenceRepository, productService);
    }

    @Test
    void deveCriarCarrinhoQuandoNaoExistirMasUsuarioJaExistir() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho novoCarrinho = new Carrinho(userRef);
        CarrinhoResponse response = mock(CarrinhoResponse.class);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.empty());
        when(userReferenceRepository.findByExternalId("user-123"))
                .thenReturn(Optional.of(userRef));
        when(carrinhoRepository.save(any(Carrinho.class)))
                .thenReturn(novoCarrinho);
        when(carrinhoMapper.toResponse(novoCarrinho))
                .thenReturn(response);

        ResponseEntity<CarrinhoResponse> result = carrinhoController.meuCarrinho(user);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(userReferenceRepository).findByExternalId("user-123");
        verify(carrinhoRepository).save(any(Carrinho.class));
        verify(carrinhoMapper).toResponse(novoCarrinho);
        verifyNoInteractions(productService);
    }

    @Test
    void deveCriarUserReferenceECarrinhoQuandoNenhumExistir() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho novoCarrinho = new Carrinho(userRef);
        CarrinhoResponse response = mock(CarrinhoResponse.class);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.empty());
        when(userReferenceRepository.findByExternalId("user-123"))
                .thenReturn(Optional.empty());
        when(userReferenceRepository.save(any(UserReference.class)))
                .thenReturn(userRef);
        when(carrinhoRepository.save(any(Carrinho.class)))
                .thenReturn(novoCarrinho);
        when(carrinhoMapper.toResponse(novoCarrinho))
                .thenReturn(response);

        ResponseEntity<CarrinhoResponse> result = carrinhoController.meuCarrinho(user);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(userReferenceRepository).findByExternalId("user-123");
        verify(userReferenceRepository).save(any(UserReference.class));
        verify(carrinhoRepository).save(any(Carrinho.class));
        verify(carrinhoMapper).toResponse(novoCarrinho);
        verifyNoInteractions(productService);
    }

    @Test
    void deveAdicionarItemAoCarrinhoExistente() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho carrinho = new Carrinho(userRef);

        Produto produto = mock(Produto.class);
        when(produto.getId()).thenReturn(10L);

        CarrinhoResponse response = mock(CarrinhoResponse.class);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.of(carrinho));
        when(productService.buscarEntidadePorId(10L))
                .thenReturn(produto);
        when(carrinhoRepository.save(carrinho))
                .thenReturn(carrinho);
        when(carrinhoMapper.toResponse(carrinho))
                .thenReturn(response);

        ResponseEntity<CarrinhoResponse> result = carrinhoController.adicionarItem(user, 10L, 2);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        assertEquals(1, carrinho.getItens().size());
        assertEquals(2, carrinho.getItens().get(0).getQuantidade());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(productService).buscarEntidadePorId(10L);
        verify(carrinhoRepository).save(carrinho);
        verify(carrinhoMapper).toResponse(carrinho);
    }

    @Test
    void deveSomarQuantidadeQuandoProdutoJaExistirNoCarrinho() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho carrinho = new Carrinho(userRef);

        Produto produto = mock(Produto.class);
        when(produto.getId()).thenReturn(10L);

        ItemCarrinho itemExistente = new ItemCarrinho(produto, 2);
        itemExistente.setCarrinho(carrinho);
        carrinho.getItens().add(itemExistente);

        CarrinhoResponse response = mock(CarrinhoResponse.class);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.of(carrinho));
        when(productService.buscarEntidadePorId(10L))
                .thenReturn(produto);
        when(carrinhoRepository.save(carrinho))
                .thenReturn(carrinho);
        when(carrinhoMapper.toResponse(carrinho))
                .thenReturn(response);

        ResponseEntity<CarrinhoResponse> result = carrinhoController.adicionarItem(user, 10L, 3);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        assertEquals(1, carrinho.getItens().size());
        assertEquals(5, carrinho.getItens().get(0).getQuantidade());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(productService).buscarEntidadePorId(10L);
        verify(carrinhoRepository).save(carrinho);
        verify(carrinhoMapper).toResponse(carrinho);
    }

    @Test
    void deveRemoverItemDoCarrinho() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho carrinho = new Carrinho(userRef);

        Produto produto = mock(Produto.class);
        when(produto.getId()).thenReturn(10L);

        ItemCarrinho item = new ItemCarrinho(produto, 2);
        item.setCarrinho(carrinho);
        carrinho.getItens().add(item);

        CarrinhoResponse response = mock(CarrinhoResponse.class);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.of(carrinho));
        when(carrinhoRepository.save(carrinho))
                .thenReturn(carrinho);
        when(carrinhoMapper.toResponse(carrinho))
                .thenReturn(response);

        ResponseEntity<CarrinhoResponse> result = carrinhoController.removerItem(user, 10L);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        assertTrue(carrinho.getItens().isEmpty());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(carrinhoRepository).save(carrinho);
        verify(carrinhoMapper).toResponse(carrinho);
        verifyNoInteractions(productService);
    }

    @Test
    void deveLimparCarrinho() {
        UserPrincipal user = new UserPrincipal("user-123", "Marcos", "marcos@email.com", "USER");
        UserReference userRef = new UserReference("user-123", "Marcos", "marcos@email.com");
        Carrinho carrinho = new Carrinho(userRef);

        Produto produto = mock(Produto.class);
        when(produto.getId()).thenReturn(10L);

        ItemCarrinho item = new ItemCarrinho(produto, 2);
        item.setCarrinho(carrinho);
        carrinho.getItens().add(item);

        when(carrinhoRepository.findByUsuarioExternalIdAndAtivoTrue("user-123"))
                .thenReturn(Optional.of(carrinho));
        when(carrinhoRepository.save(carrinho))
                .thenReturn(carrinho);

        ResponseEntity<Void> result = carrinhoController.limparCarrinho(user);

        assertEquals(204, result.getStatusCode().value());
        assertNull(result.getBody());
        assertTrue(carrinho.getItens().isEmpty());

        verify(carrinhoRepository).findByUsuarioExternalIdAndAtivoTrue("user-123");
        verify(carrinhoRepository).save(carrinho);
        verifyNoInteractions(userReferenceRepository, productService, carrinhoMapper);
    }
}
