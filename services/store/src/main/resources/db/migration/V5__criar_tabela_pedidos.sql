CREATE TYPE pedido_status AS ENUM (
    'PENDENTE',
    'AGUARDANDO_PAGAMENTO',
    'PAGO',
    'EM_SEPARACAO',
    'ENVIADO',
    'ENTREGUE',
    'CANCELADO'
    );

CREATE TABLE IF NOT EXISTS pedidos (
                                       id                BIGSERIAL       PRIMARY KEY,
                                       user_id           BIGINT          NOT NULL,       -- ID externo, sem FK se microsserviço
                                       carrinho_id       BIGINT,                         -- referência ao carrinho de origem
                                       status            pedido_status   NOT NULL DEFAULT 'PENDENTE',
                                       valor_total       DECIMAL(12,2)   NOT NULL DEFAULT 0 CHECK (valor_total >= 0),
                                       endereco_entrega  JSONB,                          -- snapshot do endereço
                                       created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                       updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                       CONSTRAINT fk_pedido_carrinho
                                           FOREIGN KEY (carrinho_id) REFERENCES carrinhos(id) ON DELETE SET NULL
);

CREATE INDEX idx_pedidos_user      ON pedidos(user_id);
CREATE INDEX idx_pedidos_status    ON pedidos(status);
CREATE INDEX idx_pedidos_carrinho  ON pedidos(carrinho_id);
