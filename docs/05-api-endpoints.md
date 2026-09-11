# DevMetrics — API REST (v1)

Base: `/api/v1`. Autenticação: `Authorization: Bearer <access_token>` (exceto `/auth/**`).

## Envelope padrão

```json
{ "success": true, "data": { }, "error": null, "meta": null }
```
```json
{ "success": false, "data": null,
  "error": { "code": "ACTIVITY_NOT_FOUND", "message": "Atividade não encontrada", "details": [] } }
```
Listas paginadas incluem `"meta": { "page":0, "size":20, "total":134, "totalPages":7 }`.

## Auth

| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/register` | cria conta. Body: email, password, displayName, timezone |
| POST | `/auth/login` | devolve accessToken (15min) + refreshToken (7d) |
| POST | `/auth/refresh` | rotaciona o refresh token |
| POST | `/auth/logout` | revoga o refresh token |

## Usuário

| Método | Rota | Descrição |
|---|---|---|
| GET | `/users/me` | perfil + status da conexão GitHub |
| PATCH | `/users/me` | displayName, timezone, weeklyGoalPoints |
| PUT | `/users/me/password` | troca de senha |

## Projetos

| Método | Rota | Descrição |
|---|---|---|
| GET | `/projects?status=active&page=0&size=20` | lista |
| POST | `/projects` | cria (gera atividade `NEW_PROJECT` automaticamente) |
| GET | `/projects/{id}` | detalhe + totais de atividade |
| PUT | `/projects/{id}` | atualiza |
| POST | `/projects/{id}/archive` | arquiva |
| DELETE | `/projects/{id}` | remove (só `source=MANUAL`) |
| PUT | `/projects/{id}/technologies` | define a lista de tecnologias |

## Atividades

| Método | Rota | Descrição |
|---|---|---|
| GET | `/activities?type=&from=&to=&projectId=&page=` | lista com filtros |
| POST | `/activities` | cria manual; calcula pontos na hora |
| GET | `/activities/{id}` | detalhe com `pointsBreakdown` |
| PUT | `/activities/{id}` | edita (recalcula pontos) |
| DELETE | `/activities/{id}` | remove (só `source=MANUAL`) |

`POST /activities` body:
```json
{ "type":"TEST", "title":"Teste do endpoint /login",
  "description":"3 casos: ok, senha errada, usuário inexistente",
  "projectId":12, "technologyId":7, "occurredAt":"2026-09-11T14:30:00Z" }
```

## Tecnologias

| Método | Rota | Descrição |
|---|---|---|
| GET | `/technologies?q=&category=` | catálogo global (autocomplete) |
| GET | `/users/me/technologies` | as do usuário, com `firstUsedAt` e `usageCount` |

## Pontuação

| Método | Rota | Descrição |
|---|---|---|
| GET | `/scoring/rules` | regras efetivas (global + override do usuário) |
| PUT | `/scoring/rules/{activityType}` | edita basePoints, dailyCap, diminishing, active |
| DELETE | `/scoring/rules/{activityType}` | remove override, volta ao padrão global |
| POST | `/scoring/recalculate` | recalcula pontos e stats do período (assíncrono) |
| GET | `/score` | Dev Score atual + breakdown por componente |
| GET | `/score/history?from=&to=` | série de snapshots |

`GET /score` response:
```json
{ "devScore": 642, "level": "Consistente", "calculatedAt": "2026-09-11T12:00:00Z",
  "breakdown": [
    { "component":"VOLUME",       "weight":0.40, "raw":312.5, "normalized":0.78, "points":312 },
    { "component":"CONSISTENCY",  "weight":0.20, "raw":41,    "normalized":0.68, "points":136 },
    { "component":"VARIETY",      "weight":0.15, "raw":0.71,  "normalized":0.71, "points":106 },
    { "component":"LEARNING",     "weight":0.15, "raw":3,     "normalized":0.60, "points":90  },
    { "component":"TECH_DIVERSITY","weight":0.10,"raw":6,     "normalized":0.55, "points":55  }
  ],
  "trend": { "delta30d": 48, "direction": "UP" } }
```

## Dashboard

| Método | Rota | Descrição |
|---|---|---|
| GET | `/dashboard/summary` | score, streak, atividades da semana, projetos ativos, top techs |
| GET | `/dashboard/calendar?year=2026` | 365 dias com `level` 0..4 |
| GET | `/dashboard/streak` | atual, recorde, diasAtivos30d |
| GET | `/dashboard/activity-breakdown?days=30` | contagem e pontos por tipo |

`GET /dashboard/calendar` response (trecho):
```json
{ "year":2026, "totalActivities":284, "totalPoints":1930,
  "days":[ { "date":"2026-01-01","count":0,"points":0,"level":0 },
           { "date":"2026-01-02","count":3,"points":21.5,"level":2 } ] }
```

## GitHub

| Método | Rota | Descrição |
|---|---|---|
| GET | `/github/authorize-url` | devolve a URL de autorização + `state` |
| GET | `/github/callback?code=&state=` | troca code por token e salva a conta |
| GET | `/github/status` | conectado?, login, lastSyncedAt, rateLimit |
| POST | `/github/sync` | dispara sync (202 Accepted + `syncId`) |
| GET | `/github/sync/{syncId}` | status do sync |
| DELETE | `/github/disconnect` | remove o token (mantém as atividades já importadas) |

## Conquistas

| Método | Rota | Descrição |
|---|---|---|
| GET | `/achievements` | catálogo completo |
| GET | `/users/me/achievements` | desbloqueadas + progresso das pendentes |

## Desafio de Hoje

| Método | Rota | Descrição |
|---|---|---|
| GET | `/challenges/today` | gera (se ainda não existir) e devolve o desafio do dia |
| POST | `/challenges/{id}/complete` | marca concluído; body opcional com `activityId` |
| POST | `/challenges/{id}/skip` | pula e sorteia outro template |
| GET | `/challenges/history?page=` | desafios anteriores |

## Próximo Passo

| Método | Rota | Descrição |
|---|---|---|
| GET | `/insights/next-step` | sugestão de tecnologia + justificativa |

```json
{ "suggestion": { "technology":"Docker", "category":"TOOL",
    "reason":"Você tem 8 projetos em Java/Spring Boot e nenhum com containerização.",
    "firstStep":"Crie um Dockerfile para o projeto devmetrics-api.",
    "estimatedMinutes":45 },
  "basedOn": { "dominantTechnologies":["Java","Spring Boot","PostgreSQL"], "projectCount":8 } }
```

## Histórico

| Método | Rota | Descrição |
|---|---|---|
| GET | `/history/monthly?year=2026` | por mês: atividades, pontos, devScore, techs novas, projetos, desafios |

## Códigos HTTP

`200` ok · `201` criado · `202` sync aceito · `204` deletado · `400` validação ·
`401` sem token/expirado · `403` recurso de outro usuário · `404` não encontrado ·
`409` conflito (e-mail duplicado, GitHub já conectado) · `429` rate limit do GitHub ·
`500` erro interno.

## Swagger

`springdoc-openapi-starter-webmvc-ui` → UI em `/swagger-ui.html`, spec em `/v3/api-docs`.
Esquema de segurança `bearerAuth` declarado em `OpenApiConfig`.
