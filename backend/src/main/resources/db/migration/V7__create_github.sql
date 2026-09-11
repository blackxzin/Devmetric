-- Conexao com o GitHub e historico de sincronizacoes.

CREATE TABLE github_accounts (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    github_user_id         BIGINT       NOT NULL,
    github_login           VARCHAR(100) NOT NULL,
    avatar_url             VARCHAR(500),
    access_token_encrypted TEXT         NOT NULL,
    scopes                 VARCHAR(200),
    connected_at           TIMESTAMPTZ  NOT NULL,
    last_synced_at         TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ,
    CONSTRAINT uk_github_account_user UNIQUE (user_id),
    CONSTRAINT fk_github_account_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE sync_logs (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    started_at         TIMESTAMPTZ NOT NULL,
    finished_at        TIMESTAMPTZ,
    status             VARCHAR(20) NOT NULL,
    repos_scanned      INTEGER     NOT NULL DEFAULT 0,
    activities_created INTEGER     NOT NULL DEFAULT 0,
    error_message      VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ,
    CONSTRAINT fk_sync_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_sync_log_user ON sync_logs (user_id, started_at DESC);
