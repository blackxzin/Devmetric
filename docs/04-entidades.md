# DevMetrics — Entidades JPA

Padrão comum: todas herdam de `BaseEntity` (id, createdAt, updatedAt via `@PrePersist`/`@PreUpdate`).
Relacionamentos sempre `FetchType.LAZY`. Sem `CascadeType.ALL` entre agregados diferentes.

## BaseEntity (common/audit)

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant updatedAt;
    // @PrePersist / @PreUpdate preenchem os timestamps
}
```

## User

```java
@Entity @Table(name = "users")
public class User extends BaseEntity {
    private String email;            // unique
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private String timezone;         // default America/Sao_Paulo
    private int weeklyGoalPoints;    // default 150
    @Enumerated(EnumType.STRING) private Role role;
}
```
Regra de domínio: `changePassword`, `updateProfile` retornam/atualizam via métodos, sem setters públicos soltos.

## GitHubAccount

```java
@Entity @Table(name = "github_accounts")
public class GitHubAccount extends BaseEntity {
    @OneToOne(fetch = LAZY) @JoinColumn(name = "user_id", unique = true) private User user;
    private Long githubUserId;
    private String githubLogin;
    private String accessTokenEncrypted;   // nunca serializado em DTO
    private String scopes;
    private Instant connectedAt;
    private Instant lastSyncedAt;
}
```

## Project

```java
@Entity @Table(name = "projects")
public class Project extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    private String name;
    private String description;
    @Enumerated(EnumType.STRING) private ProjectSource source;   // MANUAL | GITHUB
    private String externalId;
    private String repoUrl;
    private LocalDate startedAt;
    private LocalDate archivedAt;
    @ManyToMany @JoinTable(name = "project_technologies")
    private Set<Technology> technologies = new HashSet<>();
}
```

## Technology / UserTechnology

```java
@Entity public class Technology extends BaseEntity {
    private String name; private String slug;
    @Enumerated(EnumType.STRING) private TechnologyCategory category;
}

@Entity @Table(name = "user_technologies",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","technology_id"}))
public class UserTechnology extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    @ManyToOne(fetch = LAZY) private Technology technology;
    private Instant firstUsedAt;
    private int usageCount;
}
```

## Activity (agregado central)

```java
@Entity @Table(name = "activities")
public class Activity extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    @ManyToOne(fetch = LAZY) private Project project;          // nullable
    @Enumerated(EnumType.STRING) private ActivityType type;
    @Enumerated(EnumType.STRING) private ActivitySource source; // MANUAL | GITHUB
    private String externalId;                                  // SHA / número do PR
    private String title;
    private String description;
    private Instant occurredAt;
    private LocalDate activityDate;                             // derivado do timezone do user
    private BigDecimal points;                                  // congelado no momento do cálculo
    @ManyToOne(fetch = LAZY) private Technology technology;      // nullable
    @JdbcTypeCode(SqlTypes.JSON) private Map<String,Object> metadata;
}
```

Por que `points` fica gravado na atividade: o Dev Score precisa ser reproduzível.
Se o usuário mudar a regra de pontuação hoje, o passado não deve mudar sozinho —
existe um endpoint explícito de recálculo (`POST /scoring/recalculate`).

## ScoringRule

```java
@Entity @Table(name = "scoring_rules")
public class ScoringRule extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;          // null = regra global
    @Enumerated(EnumType.STRING) private ActivityType activityType;
    private BigDecimal basePoints;
    private Integer dailyCap;        // null = sem limite
    private boolean diminishing;     // aplica 1/n na n-ésima do dia
    private boolean active;
}
```

## DailyStat / ScoreSnapshot

```java
@Entity @Table(name = "daily_stats")
public class DailyStat extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    private LocalDate statDate;
    private int activityCount;
    private BigDecimal rawPoints;
    private short intensityLevel;        // 0..4 (cor do calendário)
    private short distinctTypes;
    private short distinctTechnologies;
}

@Entity @Table(name = "score_snapshots")
public class ScoreSnapshot extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    private LocalDate snapshotDate;
    private int devScore;                                    // 0..1000
    @JdbcTypeCode(SqlTypes.JSON) private Map<String,Object> breakdown;
}
```

## Achievement / UserAchievement

```java
@Entity public class Achievement extends BaseEntity {
    private String code;          // FIRST_PROJECT, STREAK_7, FIRST_DOCKER...
    private String name, description, icon;
    @Enumerated(EnumType.STRING) private AchievementCategory category;
    private Integer threshold;
    private int pointsBonus;
}

@Entity @Table(name = "user_achievements")
public class UserAchievement extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    @ManyToOne(fetch = LAZY) private Achievement achievement;
    private Instant unlockedAt;
}
```

## ChallengeTemplate / DailyChallenge

```java
@Entity public class ChallengeTemplate extends BaseEntity {
    private String code, title, descriptionTemplate;
    @Enumerated(EnumType.STRING) private ActivityType activityType;
    private int estimatedMinutes;
    @Enumerated(EnumType.STRING) private ChallengeTrigger trigger;
    @Enumerated(EnumType.STRING) private Difficulty difficulty;
    private boolean active;
}

@Entity @Table(name = "daily_challenges")
public class DailyChallenge extends BaseEntity {
    @ManyToOne(fetch = LAZY) private User user;
    @ManyToOne(fetch = LAZY) private ChallengeTemplate template;
    private LocalDate challengeDate;
    private String renderedText;
    @Enumerated(EnumType.STRING) private ChallengeStatus status;
    private Instant completedAt;
    @ManyToOne(fetch = LAZY) private Activity activity;    // atividade que comprovou
}
```

## Armadilhas a evitar

- `@ManyToMany` só entre `Project` e `Technology`. Em qualquer outro lugar, entidade de junção explícita.
- Nunca `@Enumerated(ORDINAL)`.
- Nunca `EAGER` — o dashboard faz muitas leituras e um EAGER escondido vira N+1.
- Consultas do dashboard usam **projections** (`interface` ou `record` no `@Query`), não a entidade inteira.
