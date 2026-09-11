# DevMetrics — Estrutura de Pastas

## Raiz do repositório

```
devmetrics/
├── docs/                       # documentos de planejamento (este diretório)
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/devmetrics/...
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-prod.yml
│       │   └── db/migration/      # Flyway: V1__init.sql, V2__...
│       └── test/java/com/devmetrics/...
├── frontend/
│   ├── index.html
│   ├── login.html
│   ├── dashboard.html
│   ├── css/
│   ├── js/
│   │   ├── api.js              # única camada que conhece fetch e o token
│   │   ├── auth.js
│   │   ├── dashboard.js
│   │   ├── calendar.js
│   │   └── components/
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

## Backend — package by feature

```
com/devmetrics/
├── DevMetricsApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   ├── JacksonConfig.java
│   └── SchedulingConfig.java
│
├── common/
│   ├── response/  ApiResponse.java, PageMeta.java
│   ├── exception/ BusinessException.java, NotFoundException.java,
│   │              GlobalExceptionHandler.java, ErrorCode.java
│   ├── audit/     BaseEntity.java (id, createdAt, updatedAt)
│   └── util/      DateRange.java, Slugifier.java
│
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── CurrentUser.java               # @AuthenticationPrincipal helper
│   ├── domain/ RefreshToken.java
│   ├── repository/ RefreshTokenRepository.java
│   └── dto/ RegisterRequest.java, LoginRequest.java, AuthResponse.java
│
├── user/
│   ├── UserController.java · UserService.java
│   ├── domain/ User.java
│   ├── repository/ UserRepository.java
│   └── dto/ UserResponse.java, UpdateUserRequest.java
│
├── project/
│   ├── ProjectController.java · ProjectService.java
│   ├── domain/ Project.java, ProjectSource.java
│   ├── repository/ ProjectRepository.java
│   └── dto/ ...
│
├── technology/
│   ├── TechnologyController.java · TechnologyService.java
│   ├── domain/ Technology.java, UserTechnology.java, TechnologyCategory.java
│   ├── repository/ ...
│   └── dto/ ...
│
├── activity/
│   ├── ActivityController.java · ActivityService.java
│   ├── domain/ Activity.java, ActivityType.java, ActivitySource.java
│   ├── repository/ ActivityRepository.java
│   └── dto/ CreateActivityRequest.java, ActivityResponse.java
│
├── scoring/
│   ├── ScoringController.java
│   ├── ScoringRuleService.java        # CRUD das regras
│   ├── PointsCalculator.java          # pontos de UMA atividade (com rendimento decrescente)
│   ├── DevScoreCalculator.java        # score composto 0..1000
│   ├── DailyStatsService.java         # materializa daily_stats
│   ├── domain/ ScoringRule.java, DailyStat.java, ScoreSnapshot.java
│   └── dto/ ScoreResponse.java, ScoreBreakdown.java
│
├── github/
│   ├── GitHubController.java
│   ├── GitHubOAuthService.java        # troca code por access_token
│   ├── GitHubApiClient.java           # RestClient, rate limit, paginação
│   ├── GitHubSyncService.java         # orquestra o sync
│   ├── ActivityClassifier.java        # commit -> ActivityType (heurística)
│   ├── domain/ GitHubAccount.java, SyncLog.java
│   └── dto/ (records espelhando o JSON do GitHub)
│
├── dashboard/
│   ├── DashboardController.java · DashboardService.java
│   └── dto/ DashboardSummary.java, CalendarDay.java, StreakInfo.java
│
├── achievement/
│   ├── AchievementController.java · AchievementService.java
│   ├── rules/ AchievementRule.java (interface) + implementações
│   ├── domain/ Achievement.java, UserAchievement.java
│   └── dto/ ...
│
├── challenge/
│   ├── ChallengeController.java · ChallengeService.java
│   ├── ChallengeGenerator.java        # escolhe o desafio do dia por regra
│   ├── domain/ ChallengeTemplate.java, DailyChallenge.java, ChallengeStatus.java
│   └── dto/ ...
│
├── insight/
│   ├── InsightController.java
│   ├── NextStepService.java           # sugestão de tecnologia
│   ├── domain/ TechnologySuggestionRule.java
│   └── dto/ NextStepResponse.java
│
└── history/
    ├── HistoryController.java · HistoryService.java
    └── dto/ MonthlyHistoryResponse.java
```

## Convenções

- Um arquivo por classe pública. Alvo de 200–400 linhas, máximo 800.
- Sufixos obrigatórios: `Controller`, `Service`, `Repository`, `Request`, `Response`.
- DTO de entrada: `XxxRequest`. DTO de saída: `XxxResponse`. Ambos são `record`.
- Entidade JPA nunca sai do backend. Sem exceção.
- Teste espelha o pacote da classe testada: `scoring/DevScoreCalculatorTest.java`.
