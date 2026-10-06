# DevMetrics — Integração com GitHub

Objetivo: importar atividade real e **classificá-la por tipo**, para que o score não seja
uma contagem de commits.

## 1. Conexão (OAuth Authorization Code)

Cadastro prévio de uma OAuth App em GitHub → Settings → Developer settings.
Callback URL: `http://localhost:8080/api/v1/github/callback`.

```
Usuário clica "Conectar GitHub"
  │
  ├─ GET /api/v1/github/authorize-url   (autenticado)
  │     backend gera state aleatório, guarda em cache com o userId (TTL 5min)
  │     devolve: https://github.com/login/oauth/authorize
  │               ?client_id=...&scope=read:user,repo&state=...
  │
  ├─ navegador vai ao GitHub, usuário autoriza
  │
  ├─ GitHub redireciona: GET /api/v1/github/callback?code=...&state=...
  │     backend valida o state (se não bater: 400 — proteção CSRF)
  │     POST https://github.com/login/oauth/access_token  {client_id, client_secret, code}
  │     GET  https://api.github.com/user  → id, login, avatar_url
  │     cifra o access_token (AES-GCM) e salva em github_accounts
  │
  └─ redireciona para o frontend: /dashboard.html?github=connected
```

Escopos: `read:user` e `repo`. Sem `repo` não dá para ler commits de repositório privado.
Deixar claro na tela que o acesso é somente leitura e pode ser revogado.

## 2. Sincronização

`POST /api/v1/github/sync` → 202 + `syncId`, execução em `@Async`. Grava em `sync_logs`.

Sync incremental: usa `github_accounts.last_synced_at` como `since`. Primeiro sync busca
os últimos **90 dias** (evita importar 5 anos de história e estourar o rate limit).

```
1. GET /user/repos?affiliation=owner,collaborator&sort=pushed&per_page=100
   └─ upsert em projects (source=GITHUB, external_id = repo.id)
   └─ GET /repos/{owner}/{repo}/languages  → upsert technologies + project_technologies
                                             + user_technologies (define "tech nova")

2. Para cada repo com pushed_at > since:
   GET /repos/{owner}/{repo}/commits?author={login}&since={since}&per_page=100
   └─ para cada commit: classificar (passo 3) e criar Activity
      external_id = sha  → o índice único bloqueia duplicata

3. GET /search/issues?q=author:{login}+type:pr+created:>{since}
   └─ Activity PULL_REQUEST (external_id = "pr:" + id)

4. GET /search/issues?q=author:{login}+type:issue+created:>{since}
   └─ Activity ISSUE (external_id = "issue:" + id)

5. Recalcular daily_stats e o Dev Score do período afetado.
6. Avaliar conquistas.
7. Atualizar last_synced_at e fechar o sync_log.
```

## 3. Classificação de commits (`ActivityClassifier`)

Sem IA. Ordem de precedência — o primeiro match vence:

**a) Conventional Commits** (prefixo da mensagem):
| Prefixo | ActivityType |
|---|---|
| `feat:` | FEATURE |
| `fix:` / `bugfix:` / `hotfix:` | BUG_FIX |
| `test:` | TEST |
| `docs:` | DOCUMENTATION |
| `refactor:` / `perf:` / `style:` | REFACTOR |
| `chore:` / `ci:` / `build:` | COMMIT |

**b) Caminho dos arquivos alterados** (`GET /repos/{o}/{r}/commits/{sha}` → `files[]`):
- todos sob `src/test/`, `tests/`, `__tests__/`, ou nome casando `.*Test\.\w+|.*\.spec\.\w+|test_.*\.py` → TEST
- todos com extensão `.md`, `.adoc`, `.rst`, ou sob `docs/` → DOCUMENTATION
- `Dockerfile`, `docker-compose.yml`, `.github/workflows/**` → DEPLOY

**c) Palavras-chave na mensagem** (case-insensitive, com acento normalizado):
`fix|corrige|corrigi|bug|resolve|closes #` → BUG_FIX ·
`add|implementa|cria|nova funcionalidade` → FEATURE ·
`test|teste|cobertura` → TEST · `doc|readme` → DOCUMENTATION

**d) Fallback**: `COMMIT`.

O tipo detectado vai em `metadata.classifiedBy` (`CONVENTIONAL`, `PATH`, `KEYWORD`, `FALLBACK`).
Assim dá para auditar a heurística e o usuário pode corrigir o tipo manualmente (`PUT /activities/{id}`).

Custo: buscar `files[]` é 1 request por commit. Otimização do MVP — só buscar detalhe quando
(a) e (c) não decidiram, e limitar a 200 detalhes por sync.

## 4. Detecção de tecnologia

- `GET /repos/{o}/{r}/languages` → linguagens do repo (peso em bytes).
- Arquivos-marcador: `pom.xml`→Maven, `build.gradle`→Gradle, `package.json`→Node,
  `Dockerfile`→Docker, `requirements.txt`→Python, `docker-compose.yml`→Docker Compose.
- Dependências do `pom.xml`: `spring-boot-starter-web`→Spring Boot, `spring-boot-starter-data-jpa`→JPA.
- Uma tecnologia que ainda não existe em `user_technologies` é **nova** → bônus + conquista.

## 5. Rate limit e resiliência

- Limite autenticado: 5000 req/h. Ler os headers `x-ratelimit-remaining` e `x-ratelimit-reset`
  em cada resposta e guardar no `GitHubApiClient`.
- Abaixo de 100 restantes: pausar o sync, marcar `status=PARTIAL`, retomar no próximo ciclo.
- `403` com `x-ratelimit-remaining: 0` → 429 para o cliente com o instante de reset.
- Timeouts: connect 5s, read 15s. Retry só em 5xx e 429, com backoff exponencial (3 tentativas).
- Sync agendado: `@Scheduled(cron = "0 0 */6 * * *")` para contas conectadas.
- O usuário não pode disparar sync manual mais de 1x a cada 10 minutos.

## 6. Desconexão

`DELETE /github/disconnect` apaga a linha de `github_accounts` (com o token).
As atividades importadas **permanecem** — são o histórico do usuário. Os projetos
`source=GITHUB` ficam somente leitura.

## 7. Webhook (tempo real)

`POST /api/v1/github/webhook` recebe eventos `push`. Configuração no repositório (ou na organização):
Settings → Webhooks → Payload URL `https://<host>/api/v1/github/webhook`, content type `application/json`,
secret = `GITHUB_WEBHOOK_SECRET`, evento "Just the push event".

- Assinatura `X-Hub-Signature-256` validada com HMAC-SHA256 e comparação em tempo constante.
- O payload já traz mensagem e arquivos de cada commit: classificação idêntica à do sync, sem chamada extra.
- Só entram commits cujo `author.username` é o login conectado; `sender.id` identifica a conta.
- Chave externa `commit:<sha>` igual à do sync: webhook + sync nunca duplicam.

## 8. GitLab

Mesmo pipeline (classificador, detector de tecnologia, `ActivityService.record`), outra origem:

- Autenticação por **Personal Access Token** (`read_api`), cifrado com o mesmo `TokenCipher`.
  Funciona em gitlab.com e instâncias self-hosted sem registrar OAuth App em cada uma.
- Importa projetos com atividade desde o último sync (padrão 90 dias), commits do usuário
  (casados por e-mail/nome, porque commit no GitLab não traz id do autor), merge requests e issues criados.
- Atividades e projetos ficam com `source=GITLAB` e são somente leitura, como os do GitHub.

## 9. Testes

- `ActivityClassifierTest`: tabela de casos, um por regra (é a classe com mais valor em teste unitário).
- `GitHubApiClientTest` com `MockRestServiceServer` — nunca chamar a API real em teste.
- `GitHubSyncServiceTest`: rodar o mesmo payload duas vezes e provar que não duplica atividade.
