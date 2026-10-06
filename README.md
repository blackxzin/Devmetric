# DevMetrics

[![CI](https://github.com/blackxzin/Devmetric/actions/workflows/ci.yml/badge.svg)](https://github.com/blackxzin/Devmetric/actions/workflows/ci.yml)

Plataforma para desenvolvedores acompanharem a própria evolução. Inspirada no calendário de
contribuições do GitHub, mas medindo o que realmente representa crescimento: testes,
documentação, bugs corrigidos, projetos, estudo e tecnologias novas — não só commits.

## O que tem

- **Dev Score 0–1000 explicável**: a API devolve cada componente e o peso usado.
- **Calendário de intensidade** baseado em atividade real (não em quantidade de commits).
- **GitHub**: OAuth + sync agendado + **webhook de push em tempo real** (HMAC). Commits classificados
  por heurística (teste, bug, doc, refactor...) a partir da mensagem e dos arquivos alterados.
- **GitLab**: token pessoal `read_api`, gitlab.com ou self-hosted, mesmo pipeline de classificação.
- **Conquistas**, **Desafio de Hoje** (regras; reescrito pelo **Claude** quando há `ANTHROPIC_API_KEY`)
  e **Próximo Passo** (sugestão de tecnologia por co-ocorrência).
- **Perfil público** `/u/{username}` e **card SVG para README** — opt-in, sem e-mail.
- **Conta demo** com um ano de dados para ver o produto sem criar conta.

## Card no seu README

Ative o perfil público em Configurações e cole:

```markdown
[![DevMetrics](https://SEU-HOST/api/v1/public/u/SEU-USUARIO/badge.svg)](https://SEU-HOST/u/SEU-USUARIO)
```

## Stack

Java 21 · Spring Boot 3.3 (Web, Data JPA, Security, Validation) · PostgreSQL 16 · Flyway ·
GitHub/GitLab REST · Anthropic Java SDK · HTML/CSS/JS puro (nginx) · Docker Compose ·
OpenAPI/Swagger · JUnit 5 + Testcontainers · GitHub Actions.

## Como rodar

```bash
cp .env.example .env
docker compose up -d --build
```

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080 (também em http://localhost:3000/api via proxy do nginx) |
| Swagger | http://localhost:8080/swagger-ui.html |
| Perfil demo | http://localhost:3000/u/demo (com `DEMO_ENABLED=true`) |

No login, **"Ver demo sem criar conta"** entra na conta demo.

### Testes

```bash
cd backend
mvn verify   # unitários + integração (precisa de Docker para o Testcontainers)
```

Relatório de cobertura em `backend/target/site/jacoco/index.html` (também publicado como artefato no CI).

Os testes de integração sobem a API inteira com Postgres real e provam, entre outras coisas,
que um usuário **nunca** lê nem altera dado de outro (recurso alheio responde 404).

## Deploy

`render.yaml` é um blueprint do [Render](https://render.com): banco + API + frontend.
New → Blueprint → aponte para este repositório e preencha as variáveis pedidas
(`TOKEN_ENCRYPTION_KEY` com 32 caracteres, `FRONTEND_URL`, `API_UPSTREAM` etc.).

## Variáveis opcionais

| Variável | Para quê |
|---|---|
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | conectar GitHub (OAuth App) |
| `GITHUB_WEBHOOK_SECRET` | receber push em tempo real (`/api/v1/github/webhook`) |
| `DEMO_ENABLED` | conta demo recriada no startup e toda madrugada |
| `ANTHROPIC_API_KEY` | Desafio de Hoje personalizado pelo Claude (`claude-opus-5-5`); sem chave, usa as regras |

## Ideia central

```
atividade  ->  pontos (configurável, com rendimento decrescente)
           ->  Dev Score 0–1000 = 400·Volume + 200·Consistência + 150·Variedade
                                 + 150·Aprendizado + 100·Diversidade tecnológica
```

10 commits no mesmo dia valem 14,65 pontos. 1 commit + 1 teste + 1 documentação valem 19.
Variedade paga mais que repetição — é o que impede o sistema de premiar commit inútil.

## Documentação

| Documento | Conteúdo |
|---|---|
| [00-mvp.md](docs/00-mvp.md) | problema, escopo do MVP, critérios de aceite |
| [01-arquitetura.md](docs/01-arquitetura.md) | camadas, módulos, decisões técnicas |
| [02-estrutura-pastas.md](docs/02-estrutura-pastas.md) | árvore do repositório e convenções |
| [03-modelagem-banco.md](docs/03-modelagem-banco.md) | tabelas, índices, migrations |
| [04-entidades.md](docs/04-entidades.md) | entidades JPA e armadilhas |
| [05-api-endpoints.md](docs/05-api-endpoints.md) | contrato REST completo |
| [06-autenticacao.md](docs/06-autenticacao.md) | JWT, refresh, segredos, isolamento por usuário |
| [07-integracao-github.md](docs/07-integracao-github.md) | OAuth, sync, webhook, GitLab, classificação |
| [08-sistema-pontuacao.md](docs/08-sistema-pontuacao.md) | pontos, Dev Score, anti-gaming |
| [09-plano-etapas.md](docs/09-plano-etapas.md) | roteiro de implementação e o que veio depois |
