-- Catalogo de conquistas e desbloqueios por usuario.

CREATE TABLE achievements (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(60)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(300) NOT NULL,
    icon        VARCHAR(16),
    category    VARCHAR(30)  NOT NULL,
    threshold   INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    CONSTRAINT uk_achievement_code UNIQUE (code)
);

CREATE TABLE user_achievements (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL,
    achievement_id BIGINT      NOT NULL,
    unlocked_at    TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ,
    CONSTRAINT uk_user_achievement UNIQUE (user_id, achievement_id),
    CONSTRAINT fk_user_achievement_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievement_achievement FOREIGN KEY (achievement_id) REFERENCES achievements (id)
);
