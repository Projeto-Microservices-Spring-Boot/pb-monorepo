CREATE TABLE IF NOT EXISTS user_references (
                                               id          BIGSERIAL       PRIMARY KEY,
                                               external_id VARCHAR(255)    NOT NULL UNIQUE,
                                               nome        VARCHAR(200),
                                               email       VARCHAR(200),
                                               ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
                                               created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
                                               updated_at  TIMESTAMP       DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_user_ref_external_id ON user_references(external_id);
