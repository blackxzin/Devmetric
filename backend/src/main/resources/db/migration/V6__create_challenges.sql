-- Desafio de Hoje: templates e o desafio gerado por dia.

CREATE TABLE challenge_templates (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(60)  NOT NULL,
    title                VARCHAR(140) NOT NULL,
    description_template TEXT         NOT NULL,
    activity_type        VARCHAR(30)  NOT NULL,
    estimated_minutes    INTEGER      NOT NULL,
    trigger_type         VARCHAR(30)  NOT NULL,
    difficulty           VARCHAR(20)  NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    CONSTRAINT uk_challenge_template_code UNIQUE (code)
);

CREATE TABLE daily_challenges (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL,
    template_id    BIGINT      NOT NULL,
    challenge_date DATE        NOT NULL,
    rendered_text  TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    completed_at   TIMESTAMPTZ,
    activity_id    BIGINT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ,
    CONSTRAINT uk_daily_challenge UNIQUE (user_id, challenge_date),
    CONSTRAINT fk_daily_challenge_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_challenge_template FOREIGN KEY (template_id) REFERENCES challenge_templates (id),
    CONSTRAINT fk_daily_challenge_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE SET NULL
);
