-- Regras de pontuacao configuraveis, agregado diario e historico do Dev Score.

CREATE TABLE scoring_rules (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT,
    activity_type VARCHAR(30)   NOT NULL,
    base_points   NUMERIC(6, 2) NOT NULL,
    daily_cap     INTEGER,
    diminishing   BOOLEAN       NOT NULL DEFAULT TRUE,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT uk_scoring_rule UNIQUE (user_id, activity_type),
    CONSTRAINT fk_scoring_rule_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- UNIQUE nao impede duplicata quando user_id e NULL: garante uma unica regra global por tipo.
CREATE UNIQUE INDEX uk_scoring_rule_global
    ON scoring_rules (activity_type)
    WHERE user_id IS NULL;

CREATE TABLE daily_stats (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT        NOT NULL,
    stat_date             DATE          NOT NULL,
    activity_count        INTEGER       NOT NULL DEFAULT 0,
    raw_points            NUMERIC(8, 2) NOT NULL DEFAULT 0,
    intensity_level       SMALLINT      NOT NULL DEFAULT 0,
    distinct_types        SMALLINT      NOT NULL DEFAULT 0,
    distinct_technologies SMALLINT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ,
    CONSTRAINT uk_daily_stat UNIQUE (user_id, stat_date),
    CONSTRAINT fk_daily_stat_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE score_snapshots (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    snapshot_date DATE        NOT NULL,
    dev_score     INTEGER     NOT NULL,
    breakdown     JSONB,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT uk_score_snapshot UNIQUE (user_id, snapshot_date),
    CONSTRAINT fk_score_snapshot_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
