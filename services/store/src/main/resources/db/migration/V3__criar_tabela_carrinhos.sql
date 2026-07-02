CREATE TABLE IF NOT EXISTS carrinhos (
                                         id          BIGSERIAL       PRIMARY KEY,
                                         user_id     BIGINT          NOT NULL,
                                         ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
                                         created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                         updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_carrinhos_user  ON carrinhos(user_id);
CREATE INDEX idx_carrinhos_ativo ON carrinhos(ativo);

-- 1 carrinho ativo por usuário
CREATE UNIQUE INDEX idx_carrinhos_user_ativo
    ON carrinhos(user_id)
    WHERE ativo = TRUE;
