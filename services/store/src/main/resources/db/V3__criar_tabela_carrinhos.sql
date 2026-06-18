CREATE TABLE IF NOT EXISTS carrinhos (
                                         id          BIGSERIAL       PRIMARY KEY,
                                         user_id     BIGINT          NOT NULL,
                                         ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
                                         created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                                         updated_at  TIMESTAMP       DEFAULT NOW(),

                                         CONSTRAINT fk_carrinho_user
                                             FOREIGN KEY (user_id) REFERENCES user_references(id)
);

CREATE INDEX idx_carrinhos_user ON carrinhos(user_id);
CREATE INDEX idx_carrinhos_ativo ON carrinhos(ativo);
