CREATE TABLE IF NOT EXISTS user_references (
                                               id            BIGSERIAL     PRIMARY KEY,
                                               external_id   VARCHAR(255)  NOT NULL,
                                               auth_provider VARCHAR(50)   NOT NULL DEFAULT 'KEYCLOAK',
                                               nome          VARCHAR(200)  NOT NULL,
                                               email         VARCHAR(200)  NOT NULL
                                                   CHECK (email ~* '^[^@]+@[^@]+\.[^@]+$'),
                                               ativo         BOOLEAN       NOT NULL DEFAULT TRUE,
                                               created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                               updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    -- Um mesmo external_id pode existir em providers diferentes
                                               CONSTRAINT uq_user_ref_external_provider
                                                   UNIQUE (external_id, auth_provider)
);

CREATE INDEX idx_user_ref_email ON user_references(email);
