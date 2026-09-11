-- Tabela central: toda atividade, manual ou importada do GitHub.

CREATE TABLE activities (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    project_id    BIGINT,
    type          VARCHAR(30)   NOT NULL,
    source        VARCHAR(20)   NOT NULL,
    external_id   VARCHAR(120),
    title         VARCHAR(255)  NOT NULL,
    description   TEXT,
    occurred_at   TIMESTAMPTZ   NOT NULL,
    activity_date DATE          NOT NULL,
    points        NUMERIC(7, 2) NOT NULL DEFAULT 0,
    technology_id BIGINT,
    metadata      JSONB,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_technology FOREIGN KEY (technology_id) REFERENCES technologies (id)
);

-- Deduplicacao do sync: o mesmo commit nunca entra duas vezes.
CREATE UNIQUE INDEX uk_activity_external
    ON activities (user_id, source, external_id)
    WHERE external_id IS NOT NULL;

CREATE INDEX idx_activity_user_date ON activities (user_id, activity_date);
CREATE INDEX idx_activity_user_type_date ON activities (user_id, type, activity_date);
CREATE INDEX idx_activity_project ON activities (project_id);
