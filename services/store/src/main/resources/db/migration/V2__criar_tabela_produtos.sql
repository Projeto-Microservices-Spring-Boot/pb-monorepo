CREATE TABLE IF NOT EXISTS produtos (
                                        id              BIGSERIAL       PRIMARY KEY,
                                        nome            VARCHAR(200)    NOT NULL,
                                        descricao       VARCHAR(1000),
                                        preco           DECIMAL(12,2)   NOT NULL CHECK (preco >= 0),
                                        estoque         INTEGER         NOT NULL DEFAULT 0 CHECK (estoque >= 0),
                                        imagem_url      VARCHAR(500),
                                        ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
                                        categoria_id    BIGINT          NOT NULL,
                                        created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                        updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                        CONSTRAINT fk_produto_categoria
                                            FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE RESTRICT
);

CREATE INDEX idx_produtos_ativo     ON produtos(ativo);
CREATE INDEX idx_produtos_categoria ON produtos(categoria_id);
CREATE INDEX idx_produtos_nome      ON produtos(nome);
