CREATE TABLE IF NOT EXISTS pedidos (
                                       id          BIGSERIAL       PRIMARY KEY,
                                       user_id     BIGINT          NOT NULL,
                                       status      VARCHAR(30)     NOT NULL DEFAULT 'PENDENTE',
                                       valor_total DECIMAL(12,2)   NOT NULL DEFAULT 0,
                                       created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                                       updated_at  TIMESTAMP       DEFAULT NOW(),

                                       CONSTRAINT fk_pedido_user
                                           FOREIGN KEY (user_id) REFERENCES user_references(id)
);

CREATE INDEX idx_pedidos_user ON pedidos(user_id);
CREATE INDEX idx_pedidos_status ON pedidos(status);
