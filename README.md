# DevMetrics

Plataforma para desenvolvedores acompanharem a própria evolução. Inspirada no calendário de
contribuições do GitHub, mas medindo o que realmente representa crescimento: testes,
documentação, bugs corrigidos, projetos, estudo e tecnologias novas — não só commits.

> Status: **MVP implementado**. Backend, frontend e infraestrutura estão no repositório.

## Stack

Java 21 · Spring Boot 3.3 (Web, Data JPA, Security, Validation) · PostgreSQL 16 · Flyway ·
GitHub REST API · HTML/CSS/JS puro (nginx) · Docker Compose · OpenAPI/Swagger.

## Documentação de planejamento

| Documento | Conteúdo |
|---|---|
| [00-mvp.md](docs/00-mvp.md) | problema, escopo do MVP, critérios de aceite |
| [01-arquitetura.md](docs/01-arquitetura.md) | camadas, módulos, decisões técnicas |
| [02-estrutura-pastas.md](docs/02-estrutura-pastas.md) | árvore do repositório e convenções |
| [03-modelagem-banco.md](docs/03-modelagem-banco.md) | tabelas, índices, migrations |
| [04-entidades.md](docs/04-entidades.md) | entidades JPA e armadilhas |
| [05-api-endpoints.md](docs/05-api-endpoints.md) | contrato REST completo |
| [06-autenticacao.md](docs/06-autenticacao.md) | JWT, refresh, segredos, isolamento por usuário |
| [07-integracao-github.md](docs/07-integracao-github.md) | OAuth, sync, classificação de commits |
| [08-sistema-pontuacao.md](docs/08-sistema-pontuacao.md) | pontos, Dev Score, anti-gaming |
| [09-plano-etapas.md](docs/09-plano-etapas.md) | roteiro de implementação, etapa por etapa |

## Ideia central

```
atividade  ->  pontos (configurável, com rendimento decrescente)
           ->  Dev Score 0–1000 = 400·Volume + 200·Consistência + 150·Variedade
                                 + 150·Aprendizado + 100·Diversidade tecnológica
```

10 commits no mesmo dia valem 14,65 pontos. 1 commit + 1 teste + 1 documentação valem 19.
Variedade paga mais que repetição — é o que impede o sistema de premiar commit inútil.

## Como rodar

```bash
cp .env.example .env
docker compose up -d --build
# API        http://localhost:8080
# Swagger    http://localhost:8080/swagger-ui.html
# Frontend   http://localhost:3000
```

O frontend encaminha `/api/` para o backend pelo nginx. Assim, o navegador usa o
mesmo endereço do site, inclusive ao acessar por outro computador. Para servir o
frontend fora do Compose, configure um proxy equivalente ou defina
`window.DEVMETRICS_API_URL` antes de carregar `js/api.js`.

Antes de usar fora do ambiente local, substitua as senhas e chaves do exemplo.
`TOKEN_ENCRYPTION_KEY` deve ter exatamente **32 bytes UTF-8**; gere uma chave
ASCII com `openssl rand -hex 16`. Para o JWT, use `openssl rand -hex 32`.
Não altere a chave de criptografia depois de conectar o GitHub sem planejar a
migração dos tokens existentes. A integração GitHub é opcional: configure o OAuth
App e ajuste callback, `FRONTEND_URL` e `CORS_ORIGINS` aos endereços utilizados.

## Testes

Backend (Java 21 e Maven):

```bash
cd backend
mvn -B verify
```

Frontend (Node.js 22, sem instalar dependências):

```bash
node --test frontend/tests/*.test.cjs
```

O GitHub Actions executa ambas as suítes em pushes e pull requests.
