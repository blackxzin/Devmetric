# DevMetrics — Plano de Desenvolvimento por Etapas

Cada etapa termina com algo funcionando, testado e demonstrável. Nada de "vai funcionar
quando a etapa 7 chegar". Uma etapa por vez, com revisão antes de seguir.

## Etapa 0 — Fundação (0,5 dia)

- `pom.xml`: Spring Boot 3.3, Web, Data JPA, Security, Validation, Flyway, springdoc, Lombok, driver Postgres.
- `docker-compose.yml`: `postgres:16` + `pgadmin` (opcional).
- `application.yml` + perfis `dev`/`prod`, `ddl-auto: validate`.
- `V1__create_users_and_auth.sql`.
- `GlobalExceptionHandler`, `ApiResponse`, `BaseEntity`.
- Healthcheck respondendo.

**Pronto quando:** `docker compose up -d db && mvn spring-boot:run` sobe e `/actuator/health` responde `UP`.

## Etapa 1 — Autenticação

- `User`, `RefreshToken`, repositórios.
- `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`.
- `POST /auth/register`, `/login`, `/refresh`, `/logout`, `GET /users/me`.
- Swagger com `bearerAuth`.
- Testes: unitários do `JwtService` + `@SpringBootTest` dos 4 endpoints + acesso sem token.

**Pronto quando:** dá para registrar, logar e chamar `/users/me` pelo Swagger.

## Etapa 2 — Projetos e tecnologias

- Entidades `Project`, `Technology`, `UserTechnology`, `project_technologies`.
- Seed de ~40 tecnologias (`V8`).
- CRUD de projetos + vínculo de tecnologias.
- Criar projeto gera `Activity NEW_PROJECT` (dependência: adiantar `Activity` ou criar na etapa 3 — escolha: criar na 3 e ligar aqui por evento).
- Testes de isolamento: usuário A não enxerga projeto de B.

## Etapa 3 — Atividades e motor de pontos

- `Activity`, `ActivityType`, `ScoringRule` + seeds.
- `PointsCalculator` com rendimento decrescente, dailyCap e bônus de tech nova. **TDD aqui.**
- CRUD de atividades + filtros por período/tipo/projeto.
- `GET/PUT /scoring/rules`.
- `DailyStatsService` mantém `daily_stats` a cada escrita de atividade.

**Pronto quando:** criar 10 commits no mesmo dia resulta em 14,65 pontos, não 50.

## Etapa 4 — Dev Score e dashboard

- `DevScoreCalculator` com os 5 componentes. **TDD aqui.**
- `ScoreSnapshot` + job diário.
- `GET /score`, `/score/history`.
- `GET /dashboard/summary`, `/calendar`, `/streak`, `/activity-breakdown`.
- Queries com projection (sem carregar entidade).

**Pronto quando:** o teste "mesmo volume distribuído em 5 tipos pontua mais que concentrado em 1" passa.

## Etapa 5 — Frontend base

- `login.html`, `register.html`, `dashboard.html`.
- `api.js`: fetch centralizado, injeta o Bearer, trata 401 com refresh automático, desembrulha o envelope.
- `calendar.js`: grid 53x7 em CSS Grid, 5 níveis de cor, tooltip por dia.
- Cartões: Dev Score (com breakdown), streak, atividades da semana, top tecnologias.
- Formulário de atividade manual.

**Pronto quando:** o fluxo completo funciona no navegador, sem Swagger.

## Etapa 6 — Integração GitHub

- OAuth App, `GitHubOAuthService` com `state`, cifragem do token.
- `GitHubApiClient` (RestClient, paginação, rate limit).
- `ActivityClassifier`. **TDD aqui** — tabela de casos.
- `GitHubSyncService` assíncrono + `sync_logs`.
- Botão "Conectar GitHub" e "Sincronizar agora" no frontend.

**Pronto quando:** conectar a conta e ver o calendário preencher com commits classificados por tipo.

## Etapa 7 — Conquistas

- Catálogo (`V10`) + `AchievementRule` como interface, uma implementação por regra.
- Avaliação após criar atividade e após sync.
- `GET /achievements`, `/users/me/achievements` com progresso.
- Grid de badges no frontend (bloqueada em cinza, com barra de progresso).

Conquistas iniciais: `FIRST_PROJECT`, `FIRST_PR`, `FIRST_TEST`, `FIRST_DOC`,
`STREAK_7`, `STREAK_30`, `FIRST_NEW_TECH`, `FIRST_DOCKER`, `TEN_PROJECTS`,
`HUNDRED_ACTIVITIES`, `POLYGLOT` (5 linguagens), `FULL_WEEK` (7 tipos diferentes em 7 dias).

## Etapa 8 — Anti-procrastinação

- `challenge_templates` semeados por gatilho.
- `ChallengeGenerator`: avalia os gatilhos em ordem de prioridade, escolhe 1 template,
  renderiza o texto com o contexto real (nome do projeto, endpoint mais recente sem teste).
- `GET /challenges/today`, `complete`, `skip`, `history`.
- Card "Desafio de Hoje" no dashboard.

Prioridade dos gatilhos: `INACTIVE_DAYS` (≥2 dias parado) → `STREAK_KEEPER` (streak ≥3 e hoje sem
atividade) → `NO_TESTS` (0 testes em 14d) → `NO_DOCS` → `LOW_VARIETY` (entropia < 0,3) →
`NO_NEW_TECH` (nenhuma tech nova em 30d) → padrão.

Tom: sempre tarefa pequena, com tempo estimado, sem cobrança. "Faltam X dias" nunca aparece.

## Etapa 9 — Próximo Passo

- Regras de co-ocorrência: `Java+Spring` sem `Docker` → sugerir Docker;
  sem `TEST` em 30d → sugerir JUnit; `JUnit` mas sem `Testcontainers`; sem CI → GitHub Actions;
  3+ projetos backend sem banco → PostgreSQL.
- ~~Tabela `technology_suggestion_rules` (condição em `jsonb`)~~ — decisão revista: as regras ficaram
  como lista de `Predicate` em `NextStepService`. São poucas, mudam junto com o código e uma tabela
  com condição em `jsonb` exigiria um mini-interpretador sem ganho real. Revisitar se virarem dezenas.
- `GET /insights/next-step` com justificativa e primeiro passo concreto.

## Etapa 10 — Histórico

- `GET /history/monthly` com `date_trunc('month', ...)` agregando atividades, pontos,
  snapshots de score, techs novas, projetos e desafios concluídos.
- Gráfico de linha no frontend (Canvas puro ou Chart.js via CDN).

## Etapa 11 — Empacotamento e polimento

- `Dockerfile` multi-stage do backend, `Dockerfile` nginx do frontend.
- `docker compose up` sobe tudo de uma vez.
- README com screenshots, decisões de arquitetura e instruções de execução.
- GitHub Actions: build + testes + relatório JaCoCo.
- Revisão de segurança: segredos, isolamento por usuário, validação de entrada.

## Pós-MVP

Feito:

- desafio reescrito pelo Claude (`ChallengeAiWriter`), com as regras como fallback;
- webhook do GitHub (push em tempo real, HMAC);
- perfil público `/u/{username}` + card SVG para README;
- GitLab (token pessoal, gitlab.com e self-hosted);
- conta demo, CI (GitHub Actions + JaCoCo), testes de integração com Testcontainers, blueprint do Render.

Backlog:

- migração do frontend para React, consumindo a mesma API sem alteração no backend;
- Bitbucket (mesmo pipeline do GitLab; baixa demanda no público-alvo).

## Ritmo sugerido

| Etapas | Resultado |
|---|---|
| 0–4 | backend completo e testável pelo Swagger |
| 5 | produto visível |
| 6 | dado real, o diferencial do projeto |
| 7–10 | o que transforma "CRUD bonito" em produto |
| 11 | o que faz o recrutador rodar em 1 comando |

Regra de ouro: **não começar uma etapa com a anterior sem teste.** O que vende esse projeto
numa entrevista não é a quantidade de features — é conseguir explicar por que o score é
calculado assim e mostrar o teste que prova.
