CREATE TABLE IF NOT EXISTS categorias (
                                          id          BIGSERIAL       PRIMARY KEY,
                                          nome        VARCHAR(100)    NOT NULL UNIQUE,
                                          descricao   VARCHAR(500),
                                          ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
                                          created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                                          updated_at  TIMESTAMP       DEFAULT NOW()
);

CREATE INDEX idx_categorias_ativo ON categorias(ativo);
CREATE INDEX idx_categorias_nome ON categorias(nome);
