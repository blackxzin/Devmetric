-- Conexao com o GitLab (gitlab.com ou self-hosted) via Personal Access Token read_api.

CREATE TABLE gitlab_accounts (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    gitlab_user_id         BIGINT       NOT NULL,
    username               VARCHAR(100) NOT NULL,
    avatar_url             VARCHAR(500),
    base_url               VARCHAR(200) NOT NULL,
    access_token_encrypted TEXT         NOT NULL,
    connected_at           TIMESTAMPTZ  NOT NULL,
    last_synced_at         TIMESTAMPTZ,
    last_sync_status       VARCHAR(20),
    last_sync_message      VARCHAR(500),
    last_sync_activities   INTEGER,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ,
    CONSTRAINT uk_gitlab_account_user UNIQUE (user_id),
    CONSTRAINT fk_gitlab_account_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
