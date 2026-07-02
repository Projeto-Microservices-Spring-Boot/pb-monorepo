CREATE TABLE IF NOT EXISTS itens_carrinho (
                                              id              BIGSERIAL       PRIMARY KEY,
                                              carrinho_id     BIGINT          NOT NULL,
                                              produto_id      BIGINT          NOT NULL,  -- ID externo, sem FK se for outro serviço
                                              nome_produto    VARCHAR(200),              -- snapshot
                                              imagem_url      VARCHAR(500),              -- snapshot
                                              quantidade      INTEGER         NOT NULL CHECK (quantidade >= 1),
                                              preco_unitario  DECIMAL(12,2)   NOT NULL CHECK (preco_unitario >= 0),
                                              created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                              updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                              CONSTRAINT fk_item_carrinho_carrinho
                                                  FOREIGN KEY (carrinho_id) REFERENCES carrinhos(id) ON DELETE CASCADE
);

-- 1 produto por carrinho (sem duplicatas)
CREATE UNIQUE INDEX uq_item_produto_carrinho
    ON itens_carrinho(carrinho_id, produto_id);

CREATE INDEX idx_itens_carrinho_carrinho ON itens_carrinho(carrinho_id);
CREATE INDEX idx_itens_carrinho_produto  ON itens_carrinho(produto_id);
