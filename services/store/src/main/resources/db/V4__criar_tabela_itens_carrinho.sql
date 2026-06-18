CREATE TABLE IF NOT EXISTS itens_carrinho (
                                              id              BIGSERIAL       PRIMARY KEY,
                                              carrinho_id     BIGINT          NOT NULL,
                                              produto_id      BIGINT          NOT NULL,
                                              quantidade      INTEGER         NOT NULL,
                                              preco_unitario  DECIMAL(12,2)   NOT NULL,

                                              CONSTRAINT fk_item_carrinho_carrinho
                                                  FOREIGN KEY (carrinho_id) REFERENCES carrinhos(id) ON DELETE CASCADE,

                                              CONSTRAINT fk_item_carrinho_produto
                                                  FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE INDEX idx_itens_carrinho_carrinho ON itens_carrinho(carrinho_id);
