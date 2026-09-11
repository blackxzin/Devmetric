# DevMetrics — Fluxo de Autenticação

Duas coisas diferentes, que não devem ser confundidas:

1. **Autenticação na plataforma** — e-mail + senha, com JWT. É o login do DevMetrics.
2. **Autorização no GitHub** — OAuth, só para ler dados. Não é login (ver `07-integracao-github.md`).

Separar os dois evita que o usuário perca a conta se revogar o acesso no GitHub.

## 1. Registro

```
POST /api/v1/auth/register
  ├─ valida e-mail (formato + não existe) e senha (mín. 8, letra + número)
  ├─ passwordHash = BCrypt(senha, força 10)
  ├─ salva User (timezone default America/Sao_Paulo)
  ├─ semeia scoring_rules padrão? NÃO — usa as globais (user_id NULL)
  └─ 201 + accessToken + refreshToken
```

## 2. Login

```
POST /api/v1/auth/login { email, password }
  ├─ busca usuário por e-mail
  ├─ BCrypt.matches(senha, hash)  → falhou: 401 "Credenciais inválidas"
  │                                 (mesma mensagem para e-mail inexistente,
  │                                  para não revelar quais e-mails existem)
  ├─ gera accessToken  (HS256, exp 15min)
  ├─ gera refreshToken (UUID aleatório, exp 7d)
  │    └─ guarda apenas o SHA-256 do token na tabela refresh_tokens
  └─ 200 { accessToken, refreshToken, expiresIn: 900, user: {...} }
```

Claims do access token:
```json
{ "sub": "42", "email": "dev@email.com", "role": "USER",
  "iat": 1757601600, "exp": 1757602500 }
```
O `sub` é o id numérico — nada de dados mutáveis dentro do token.

## 3. Requisição autenticada

```
Request  →  JwtAuthenticationFilter (OncePerRequestFilter)
              ├─ lê header Authorization: Bearer ...
              ├─ valida assinatura + expiração
              ├─ carrega o usuário (cache simples em memória, TTL curto)
              └─ popula SecurityContextHolder
           →  SecurityFilterChain (authorizeHttpRequests)
           →  Controller
```

Nos controllers, o usuário vem por `@AuthenticationPrincipal`, nunca por parâmetro de rota.
Regra dura: **nenhum endpoint recebe `userId` do cliente.** Todo `Repository` filtra por
`user_id` do contexto. É assim que se evita IDOR (acessar dados de outro usuário pelo id).

## 4. Refresh

```
POST /api/v1/auth/refresh { refreshToken }
  ├─ hash = SHA-256(refreshToken); busca em refresh_tokens
  ├─ inválido / expirado / revogado → 401
  ├─ ROTAÇÃO: revoga o token atual e emite um novo par
  └─ 200 { accessToken, refreshToken }
```
Rotação obrigatória: se um refresh token vazar e for usado duas vezes, a segunda falha.

## 5. Logout

`POST /auth/logout` → marca `revoked_at`. O access token continua válido até expirar
(natureza do stateless); com 15 minutos de vida isso é aceitável para este projeto.

## SecurityConfig

```java
http
  .csrf(csrf -> csrf.disable())                 // API stateless com Bearer token
  .cors(cors -> cors.configurationSource(corsSource))
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/v1/auth/**", "/api/v1/github/callback").permitAll()
      .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
      .anyRequest().authenticated())
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
  .exceptionHandling(e -> e
      .authenticationEntryPoint(jsonUnauthorizedHandler)   // 401 no envelope padrão
      .accessDeniedHandler(jsonForbiddenHandler));         // 403 no envelope padrão
```

## Segredos

Nunca no `application.yml` versionado. Sempre variáveis de ambiente:

| Variável | Uso |
|---|---|
| `JWT_SECRET` | chave HS256, mín. 32 bytes aleatórios |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | tempos de vida |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | OAuth App |
| `TOKEN_ENCRYPTION_KEY` | AES-GCM 256 para cifrar o token do GitHub em repouso |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | banco |

A aplicação falha no startup se qualquer uma faltar (`@ConfigurationProperties` + `@Validated`).
`.env.example` no repositório com os nomes e valores fake; `.env` no `.gitignore`.

## Armazenamento do token no frontend

MVP: `localStorage` + envio no header pelo `api.js`. É simples e suficiente para portfólio,
mas vulnerável a XSS — por isso todo conteúdo dinâmico é inserido com `textContent`,
nunca `innerHTML`. Evolução natural (fase futura): refresh token em cookie `HttpOnly; Secure; SameSite=Strict`.

## Testes obrigatórios

- login com senha errada → 401 e mesma mensagem do e-mail inexistente;
- acesso sem token → 401; token expirado → 401; token adulterado → 401;
- usuário A tentando ler atividade do usuário B → 404 (não 403: não confirma existência);
- reuso de refresh token já rotacionado → 401.
