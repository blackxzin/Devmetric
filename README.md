# DevMetrics

Plataforma para desenvolvedores acompanharem a própria evolução. Inspirada no calendário de
contribuições do GitHub, mas medindo o que realmente representa crescimento: testes,
documentação, bugs corrigidos, projetos, estudo e tecnologias novas — não só commits.

> Status: **planejamento concluído**. Implementação começa na Etapa 0.

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

## Como rodar (a partir da Etapa 0)

```bash
cp .env.example .env
docker compose up -d
# API        http://localhost:8080
# Swagger    http://localhost:8080/swagger-ui.html
# Frontend   http://localhost:3000
```
