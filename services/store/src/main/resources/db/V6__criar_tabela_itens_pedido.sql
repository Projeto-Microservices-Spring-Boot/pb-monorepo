CREATE TABLE IF NOT EXISTS itens_pedido (
                                            id              BIGSERIAL       PRIMARY KEY,
                                            pedido_id       BIGINT          NOT NULL,
                                            produto_id      BIGINT          NOT NULL,
                                            quantidade      INTEGER         NOT NULL,
                                            preco_unitario  DECIMAL(12,2)   NOT NULL,

                                            CONSTRAINT fk_item_pedido_pedido
                                                FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,

                                            CONSTRAINT fk_item_pedido_produto
                                                FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE INDEX idx_itens_pedido_pedido ON itens_pedido(pedido_id);
