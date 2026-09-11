# DevMetrics — Modelagem do Banco (PostgreSQL)

## Diagrama lógico

```
users 1───N projects           users 1───N activities         users 1───1 github_accounts
  │                               │                             │
  │ 1───N user_technologies       │ N───1 projects              │ 1───N sync_logs
  │        └── N───1 technologies │ N───1 technologies (opcional)
  │                               │
  │ 1───N daily_stats             │
  │ 1───N score_snapshots
  │ 1───N user_achievements ── N───1 achievements
  │ 1───N daily_challenges  ── N───1 challenge_templates
  │ 1───N refresh_tokens
  │ 1───N scoring_rules (override do usuário; regra global tem user_id NULL)

projects N───N technologies  (project_technologies)
```

## Tabelas

### users
| coluna | tipo | notas |
|---|---|---|
| id | BIGSERIAL PK | |
| email | VARCHAR(180) UNIQUE NOT NULL | |
| password_hash | VARCHAR(100) NOT NULL | BCrypt |
| display_name | VARCHAR(80) NOT NULL | |
| avatar_url | VARCHAR(500) | |
| timezone | VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo' | define o "dia" do calendário |
| weekly_goal_points | INT NOT NULL DEFAULT 150 | usado no cálculo de volume |
| role | VARCHAR(20) NOT NULL DEFAULT 'USER' | |
| created_at / updated_at | TIMESTAMPTZ | |

### refresh_tokens
`id, user_id FK, token_hash VARCHAR(64) UNIQUE, expires_at, revoked_at, created_at`
Índice: `(user_id)`, `(token_hash)`.

### github_accounts
| coluna | tipo | notas |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT UNIQUE FK users | 1:1 |
| github_user_id | BIGINT NOT NULL | id numérico, estável |
| github_login | VARCHAR(100) NOT NULL | |
| access_token_encrypted | TEXT NOT NULL | AES-GCM, chave em variável de ambiente |
| scopes | VARCHAR(200) | |
| connected_at | TIMESTAMPTZ | |
| last_synced_at | TIMESTAMPTZ | usado como `since` no sync incremental |

### technologies (catálogo global)
`id, name VARCHAR(80) UNIQUE, slug UNIQUE, category VARCHAR(30), created_at`
`category`: LANGUAGE, FRAMEWORK, DATABASE, TOOL, CLOUD, TESTING, OTHER.
Seed inicial via Flyway (Java, Spring Boot, PostgreSQL, Docker, JUnit, React...).

### user_technologies
`id, user_id FK, technology_id FK, first_used_at TIMESTAMPTZ NOT NULL, usage_count INT NOT NULL DEFAULT 1`
UNIQUE `(user_id, technology_id)` — é essa linha que define "tecnologia nova".

### projects
| coluna | tipo | notas |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK | |
| name | VARCHAR(140) NOT NULL | |
| description | TEXT | |
| source | VARCHAR(20) NOT NULL | MANUAL \| GITHUB |
| external_id | VARCHAR(100) | id do repo no GitHub |
| repo_url | VARCHAR(500) | |
| started_at | DATE NOT NULL | |
| archived_at | DATE | |
UNIQUE `(user_id, source, external_id)` — evita duplicar repo no sync.

### project_technologies
`project_id FK, technology_id FK` — PK composta.

### activities  (tabela central)
| coluna | tipo | notas |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK NOT NULL | |
| project_id | BIGINT FK NULL | |
| type | VARCHAR(30) NOT NULL | ver enum abaixo |
| source | VARCHAR(20) NOT NULL | MANUAL \| GITHUB |
| external_id | VARCHAR(120) | SHA do commit, número do PR/issue |
| title | VARCHAR(255) NOT NULL | |
| description | TEXT | |
| occurred_at | TIMESTAMPTZ NOT NULL | momento real do evento |
| activity_date | DATE NOT NULL | `occurred_at` convertido pro timezone do usuário |
| points | NUMERIC(7,2) NOT NULL DEFAULT 0 | pontos já aplicados (congelados) |
| technology_id | BIGINT FK NULL | tecnologia principal do evento |
| metadata | JSONB | additions, deletions, files, repo, labels |
| created_at | TIMESTAMPTZ | |

Índices:
- UNIQUE `(user_id, source, external_id)` WHERE `external_id IS NOT NULL` — **deduplicação do sync**.
- `(user_id, activity_date)` — calendário e streak.
- `(user_id, type, activity_date)` — rendimento decrescente e variedade.

### scoring_rules
| coluna | tipo | notas |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK NULL | NULL = regra padrão global |
| activity_type | VARCHAR(30) NOT NULL | |
| base_points | NUMERIC(6,2) NOT NULL | |
| daily_cap | INT | máx. de atividades desse tipo pontuadas por dia |
| diminishing | BOOLEAN NOT NULL DEFAULT TRUE | aplica 1/n |
| active | BOOLEAN NOT NULL DEFAULT TRUE | |
UNIQUE `(user_id, activity_type)`. Lookup: regra do usuário, senão a global.

### daily_stats  (tabela materializada, recalculável)
`id, user_id FK, stat_date DATE, activity_count INT, raw_points NUMERIC(8,2), intensity_level SMALLINT (0..4), distinct_types SMALLINT, distinct_technologies SMALLINT`
UNIQUE `(user_id, stat_date)`. É o que alimenta o calendário — uma query, sem agregação pesada.

### score_snapshots
`id, user_id FK, snapshot_date DATE, dev_score INT, breakdown JSONB, created_at`
UNIQUE `(user_id, snapshot_date)`. `breakdown` guarda cada componente e o peso do dia,
para o histórico continuar explicável mesmo se a fórmula mudar depois.

### achievements (catálogo)
`id, code VARCHAR(60) UNIQUE, name, description, icon, category, threshold INT, points_bonus INT`

### user_achievements
`id, user_id FK, achievement_id FK, unlocked_at TIMESTAMPTZ` — UNIQUE `(user_id, achievement_id)`.

### challenge_templates
`id, code UNIQUE, title, description_template TEXT, activity_type VARCHAR(30), estimated_minutes INT, trigger VARCHAR(40), difficulty VARCHAR(20), active BOOLEAN`
`trigger`: INACTIVE_DAYS, NO_TESTS, NO_DOCS, NO_NEW_TECH, LOW_VARIETY, STREAK_KEEPER.

### daily_challenges
`id, user_id FK, template_id FK, challenge_date DATE, rendered_text TEXT, status VARCHAR(20), completed_at, activity_id FK NULL`
UNIQUE `(user_id, challenge_date)`. `status`: PENDING, COMPLETED, SKIPPED, EXPIRED.

### sync_logs
`id, user_id FK, started_at, finished_at, status VARCHAR(20), repos_scanned INT, activities_created INT, error_message TEXT`

## Enum ActivityType

`COMMIT, PULL_REQUEST, ISSUE, FEATURE, BUG_FIX, TEST, DOCUMENTATION, STUDY, NEW_PROJECT, REFACTOR, CODE_REVIEW, DEPLOY`

Guardar como `VARCHAR` com `@Enumerated(EnumType.STRING)`. Nunca `ORDINAL` — reordenar o
enum corromperia os dados históricos.

## Migrations (Flyway)

```
V1__create_users_and_auth.sql
V2__create_projects_and_technologies.sql
V3__create_activities.sql
V4__create_scoring.sql
V5__create_achievements.sql
V6__create_challenges.sql
V7__create_github.sql
V8__seed_technologies.sql
V9__seed_scoring_rules.sql
V10__seed_achievements_and_challenges.sql
```
