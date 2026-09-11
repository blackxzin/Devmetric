# DevMetrics — Arquitetura do Sistema

## Estilo arquitetural

**Monolito modular em camadas**, organizado por feature (package-by-feature), não por tipo.

Motivo: é a arquitetura que um time real usa em um produto desse tamanho. Microserviços
aqui seriam complexidade artificial. A modularização por feature já deixa o caminho aberto
para extrair um módulo no futuro, se houver motivo real.

## Camadas

```
HTTP  ->  Controller  ->  Service  ->  Repository  ->  PostgreSQL
             |              |
             DTO         Domain (Entity)
```

Regras de dependência (sempre de fora para dentro):

- `Controller` conhece `Service` e DTOs. Nunca toca em `Repository` nem devolve entidade JPA.
- `Service` contém a regra de negócio e a transação (`@Transactional`). Nunca conhece HTTP.
- `Repository` só fala com o banco (Spring Data JPA).
- `Domain` (entidades) não depende de nada das camadas acima.
- `Mapper` converte Entity <-> DTO (MapStruct ou método estático simples).

## Visão de componentes

```
                    ┌──────────────────────────┐
   Browser  ───────►│  frontend (nginx)        │
                    │  HTML + CSS + JS puro    │
                    └───────────┬──────────────┘
                                │ REST /api/v1 (JWT)
                    ┌───────────▼──────────────┐
                    │  devmetrics-api          │
                    │  Spring Boot 3           │
                    │                          │
                    │  auth · user · activity  │
                    │  project · technology    │
                    │  scoring · github        │
                    │  dashboard · achievement │
                    │  challenge · history     │
                    └─────┬───────────────┬────┘
                          │               │ HTTPS
                    ┌─────▼─────┐   ┌─────▼──────────┐
                    │PostgreSQL │   │  GitHub API    │
                    └───────────┘   └────────────────┘
```

## Módulos e responsabilidades

| Módulo | Responsabilidade |
|---|---|
| `auth` | registro, login, JWT, refresh token, filtro de segurança |
| `user` | perfil, preferências, configuração de metas |
| `project` | projetos do usuário, origem (manual ou GitHub) |
| `technology` | catálogo de tecnologias, primeira utilização por usuário |
| `activity` | registro de atividades, deduplicação, consulta por período |
| `scoring` | regras configuráveis, cálculo de pontos, Dev Score, snapshots |
| `github` | OAuth, cliente REST, sync, classificação de commits em atividades |
| `dashboard` | agregações de leitura (calendário, streak, resumo) |
| `achievement` | catálogo de conquistas e avaliação de regras |
| `challenge` | geração e acompanhamento do Desafio de Hoje |
| `insight` | Próximo Passo (sugestão de tecnologia) |
| `history` | séries mensais de evolução |

## Decisões técnicas (ADR resumido)

1. **Java 21 + Spring Boot 3.3** — LTS, records e pattern matching ajudam nos DTOs.
2. **Maven** — mais comum em vagas de estágio Java que Gradle.
3. **PostgreSQL 16** — precisa de `date_trunc`, `generate_series` e `jsonb` para as agregações.
4. **Flyway** — migrations versionadas. `ddl-auto=validate` em todos os ambientes.
5. **JWT stateless + refresh token em banco** — permite revogar sessão sem sessão HTTP.
6. **Frontend separado (nginx)** — obriga API a ser realmente REST e facilita trocar por React depois.
7. **Sync do GitHub sob demanda + agendado** — sem webhook no MVP (não exige URL pública).
8. **Sem IA no MVP** — todas as recomendações são regras determinísticas e testáveis.

## Padrões transversais

- **Envelope de resposta**: toda resposta usa `{ success, data, error, meta }`.
- **Erros**: `@RestControllerAdvice` central traduz exceções em `ProblemDetail`-like.
- **Validação**: Bean Validation (`@Valid`) em todo DTO de entrada.
- **Imutabilidade**: DTOs são `record`. Entidades mudam só por métodos de domínio.
- **Paginação**: toda listagem devolve `Page` com `meta.total/page/size`.
- **Fuso horário**: tudo em UTC no banco (`Instant`); o dia do calendário usa o timezone do usuário.

## Ambientes

| Ambiente | Perfil Spring | Banco |
|---|---|---|
| local | `dev` | Postgres via Docker Compose |
| teste | `test` | Testcontainers (Postgres real) |
| produção | `prod` | Postgres gerenciado, variáveis por env |
