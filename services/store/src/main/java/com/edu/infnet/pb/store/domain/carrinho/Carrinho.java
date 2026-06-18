package com.edu.infnet.pb.store.domain.carrinho;

import com.edu.infnet.pb.store.domain.usuario.UserReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carrinhos")
public class Carrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserReference usuario;

    @OneToMany(mappedBy = "carrinho", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrinho> itens = new ArrayList<>();

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === Construtores ===

    public Carrinho() {}

    public Carrinho(UserReference usuario) {
        this.usuario = usuario;
        this.ativo = true;
    }

    // === Métodos de negócio ===

    public void adicionarItem(ItemCarrinho item) {
        // Verifica se o produto já está no carrinho
        for (ItemCarrinho existente : itens) {
            if (existente.getProduto().getId().equals(item.getProduto().getId())) {
                existente.setQuantidade(existente.getQuantidade() + item.getQuantidade());
                return;
            }
        }
        item.setCarrinho(this);
        this.itens.add(item);
    }

    public void removerItem(Long produtoId) {
        this.itens.removeIf(item -> item.getProduto().getId().equals(produtoId));
    }

    public void limpar() {
        this.itens.clear();
    }

    // === Getters e Setters ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserReference getUsuario() { return usuario; }
    public void setUsuario(UserReference usuario) { this.usuario = usuario; }

    public List<ItemCarrinho> getItens() { return itens; }
    public void setItens(List<ItemCarrinho> itens) { this.itens = itens; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
