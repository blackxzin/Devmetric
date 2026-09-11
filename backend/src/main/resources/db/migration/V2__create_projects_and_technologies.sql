-- Catalogo de tecnologias, primeira utilizacao por usuario e projetos.

CREATE TABLE technologies (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    slug       VARCHAR(80) NOT NULL,
    category   VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_technology_name UNIQUE (name),
    CONSTRAINT uk_technology_slug UNIQUE (slug)
);

-- Esta tabela e o que define "tecnologia nova" para um usuario.
CREATE TABLE user_technologies (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    technology_id BIGINT      NOT NULL,
    first_used_at TIMESTAMPTZ NOT NULL,
    usage_count   INTEGER     NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT uk_user_technology UNIQUE (user_id, technology_id),
    CONSTRAINT fk_user_technology_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_technology_tech FOREIGN KEY (technology_id) REFERENCES technologies (id)
);

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(140) NOT NULL,
    description TEXT,
    source      VARCHAR(20)  NOT NULL,
    external_id VARCHAR(100),
    repo_url    VARCHAR(500),
    started_at  DATE         NOT NULL,
    archived_at DATE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    CONSTRAINT uk_project_external UNIQUE (user_id, source, external_id),
    CONSTRAINT fk_project_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_project_user ON projects (user_id);

CREATE TABLE project_technologies (
    project_id    BIGINT NOT NULL,
    technology_id BIGINT NOT NULL,
    PRIMARY KEY (project_id, technology_id),
    CONSTRAINT fk_project_tech_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_tech_tech FOREIGN KEY (technology_id) REFERENCES technologies (id)
);
