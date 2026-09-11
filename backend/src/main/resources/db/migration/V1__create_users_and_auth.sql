-- Usuarios e autenticacao.

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(180) NOT NULL,
    password_hash       VARCHAR(100) NOT NULL,
    display_name        VARCHAR(80)  NOT NULL,
    avatar_url          VARCHAR(500),
    timezone            VARCHAR(64)  NOT NULL DEFAULT 'America/Sao_Paulo',
    weekly_goal_points  INTEGER      NOT NULL DEFAULT 150,
    role                VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);
