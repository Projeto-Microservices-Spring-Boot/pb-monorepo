CREATE TABLE IF NOT EXISTS itens_pedido (
                                            id              BIGSERIAL       PRIMARY KEY,
                                            pedido_id       BIGINT          NOT NULL,
                                            produto_id      BIGINT          NOT NULL,           -- ID externo, sem FK se microsserviço
                                            nome_produto    VARCHAR(200)    NOT NULL,            -- snapshot obrigatório
                                            sku             VARCHAR(100),                        -- snapshot do código do produto
                                            imagem_url      VARCHAR(500),                        -- snapshot opcional
                                            quantidade      INTEGER         NOT NULL CHECK (quantidade >= 1),
                                            preco_unitario  DECIMAL(12,2)   NOT NULL CHECK (preco_unitario >= 0),
                                            subtotal        DECIMAL(12,2)   GENERATED ALWAYS AS (quantidade * preco_unitario) STORED,
                                            created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                            CONSTRAINT fk_item_pedido_pedido
                                                FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE
);

-- 1 produto por pedido (sem duplicatas)
CREATE UNIQUE INDEX uq_item_produto_pedido
    ON itens_pedido(pedido_id, produto_id);

CREATE INDEX idx_itens_pedido_pedido  ON itens_pedido(pedido_id);
CREATE INDEX idx_itens_pedido_produto ON itens_pedido(produto_id);
