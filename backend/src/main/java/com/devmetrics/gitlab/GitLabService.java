package com.devmetrics.gitlab;

import com.devmetrics.activity.ActivityService;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.common.event.ActivityRecordedEvent;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.config.AppProperties;
import com.devmetrics.github.ActivityClassifier;
import com.devmetrics.github.TechnologyDetector;
import com.devmetrics.github.TokenCipher;
import com.devmetrics.gitlab.domain.GitLabAccount;
import com.devmetrics.gitlab.dto.ConnectGitLabRequest;
import com.devmetrics.gitlab.dto.GitLabStatusResponse;
import com.devmetrics.gitlab.repository.GitLabAccountRepository;
import com.devmetrics.project.ProjectService;
import com.devmetrics.project.domain.Project;
import com.devmetrics.project.domain.ProjectSource;
import com.devmetrics.scoring.ScoreService;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Conexao e importacao do GitLab. Mesmo pipeline do GitHub: classificador, detector
 * de tecnologia e ActivityService.record. Muda so a origem dos dados.
 *
 * Autenticacao por Personal Access Token (read_api) em vez de OAuth: funciona igual em
 * gitlab.com e em instancias self-hosted, sem registrar aplicacao em cada uma.
 */
@Service
public class GitLabService {

    private static final Logger log = LoggerFactory.getLogger(GitLabService.class);

    private final GitLabAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final GitLabApiClient apiClient;
    private final TokenCipher tokenCipher;
    private final ActivityClassifier classifier;
    private final TechnologyDetector technologyDetector;
    private final TechnologyService technologyService;
    private final ProjectService projectService;
    private final ActivityService activityService;
    private final ScoreService scoreService;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties.GitLab config;

    public GitLabService(GitLabAccountRepository accountRepository,
                         UserRepository userRepository,
                         GitLabApiClient apiClient,
                         TokenCipher tokenCipher,
                         ActivityClassifier classifier,
                         TechnologyDetector technologyDetector,
                         TechnologyService technologyService,
                         ProjectService projectService,
                         ActivityService activityService,
                         ScoreService scoreService,
                         ApplicationEventPublisher eventPublisher,
                         AppProperties properties) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.apiClient = apiClient;
        this.tokenCipher = tokenCipher;
        this.classifier = classifier;
        this.technologyDetector = technologyDetector;
        this.technologyService = technologyService;
        this.projectService = projectService;
        this.activityService = activityService;
        this.scoreService = scoreService;
        this.eventPublisher = eventPublisher;
        this.config = properties.gitlab();
    }

    @Transactional(readOnly = true)
    public GitLabStatusResponse status(Long userId) {
        return accountRepository.findByUserId(userId)
                .map(GitLabStatusResponse::from)
                .orElseGet(GitLabStatusResponse::disconnected);
    }

    @Transactional
    public GitLabStatusResponse connect(Long userId, ConnectGitLabRequest request) {
        String baseUrl = request.baseUrl() == null || request.baseUrl().isBlank()
                ? config.defaultBaseUrl() : request.baseUrl().strip();
        String token = request.token().strip();
        JsonNode gitlabUser = apiClient.currentUser(baseUrl, token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        accountRepository.findByUserId(userId).ifPresent(existing -> {
            accountRepository.delete(existing);
            accountRepository.flush();
        });
        GitLabAccount account = accountRepository.save(GitLabAccount.connect(user,
                gitlabUser.path("id").asLong(), gitlabUser.path("username").asText(),
                gitlabUser.path("avatar_url").asText(null), baseUrl, tokenCipher.encrypt(token)));
        return GitLabStatusResponse.from(account);
    }

    @Transactional
    public void disconnect(Long userId) {
        GitLabAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITLAB_NOT_CONNECTED));
        accountRepository.delete(account); // atividades importadas ficam: sao historico
    }

    @Transactional
    public Long markRunning(Long userId) {
        GitLabAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITLAB_NOT_CONNECTED));
        // Sem trava de "RUNNING": um crash no meio deixaria a conta presa. Dois syncs
        // simultaneos nao duplicam nada, a chave externa deduplica.
        account.markRunning();
        return account.getId();
    }

    /**
     * Fora de transacao de proposito (dezenas de chamadas HTTP); cada gravacao usa a
     * transacao curta do servico correspondente, como no sync do GitHub.
     */
    public void execute(Long accountId) {
        GitLabAccount account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return;
        }
        User user = userRepository.findById(account.getUser().getId()).orElseThrow();
        try {
            int created = importAll(user, account);
            account.markFinished(Instant.now(), created);
            scoreService.snapshot(user, LocalDate.now(user.zoneId()));
            eventPublisher.publishEvent(new ActivityRecordedEvent(user.getId()));
        } catch (BusinessException ex) {
            account.markFailed(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Erro no sync do GitLab para o usuario {}", user.getId(), ex);
            account.markFailed("Erro inesperado durante a sincronizacao");
        }
        accountRepository.save(account);
    }

    private int importAll(User user, GitLabAccount account) {
        String baseUrl = account.getBaseUrl();
        String token = tokenCipher.decrypt(account.getAccessTokenEncrypted());
        Instant since = account.getLastSyncedAt() != null
                ? account.getLastSyncedAt()
                : Instant.now().minus(Duration.ofDays(config.firstSyncDays()));

        JsonNode me = apiClient.currentUser(baseUrl, token);
        Set<String> myIdentities = identities(me);

        Map<Long, Project> projectsById = new HashMap<>();
        int detailBudget = config.maxCommitDetailsPerSync();
        int created = 0;

        for (JsonNode gitlabProject : apiClient.projects(baseUrl, token, since, config.maxProjectsPerSync())) {
            if (gitlabProject.hasNonNull("forked_from_project")) {
                continue;
            }
            long projectId = gitlabProject.path("id").asLong();
            ImportedProject imported = importProject(user, baseUrl, token, gitlabProject);
            Project project = imported.project();
            Technology mainTechnology = imported.mainLanguage();
            projectsById.put(projectId, project);

            for (JsonNode commit : apiClient.commits(baseUrl, token, projectId, since)) {
                if (!isMine(commit, myIdentities)) {
                    continue;
                }
                String sha = commit.path("id").asText();
                String message = commit.path("message").asText(commit.path("title").asText(""));
                List<String> files = List.of();
                if (detailBudget > 0 && classifier.needsFileInspection(message)) {
                    detailBudget--;
                    files = apiClient.commitFiles(baseUrl, token, projectId, sha);
                }
                ActivityClassifier.Classification classification = classifier.classify(message, files);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("repository", gitlabProject.path("path_with_namespace").asText());
                metadata.put("sha", sha);
                metadata.put("url", commit.path("web_url").asText());
                metadata.put("classifiedBy", classification.classifiedBy());

                if (activityService.record(user, classification.type(), ActivitySource.GITLAB,
                        commit.path("title").asText("Commit"), message,
                        instant(commit.path("committed_date").asText()), project, mainTechnology,
                        "commit:" + sha, metadata) != null) {
                    created++;
                }
            }
        }

        created += importCreated(user, baseUrl, token, "merge_requests", ActivityType.PULL_REQUEST, "mr:",
                since, projectsById);
        created += importCreated(user, baseUrl, token, "issues", ActivityType.ISSUE, "issue:",
                since, projectsById);
        return created;
    }

    private record ImportedProject(Project project, Technology mainLanguage) {
    }

    private ImportedProject importProject(User user, String baseUrl, String token, JsonNode gitlabProject) {
        long projectId = gitlabProject.path("id").asLong();
        LocalDate createdAt = gitlabProject.hasNonNull("created_at")
                ? instant(gitlabProject.path("created_at").asText()).atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now(user.zoneId());
        Project project = projectService.upsertImported(user, ProjectSource.GITLAB, String.valueOf(projectId),
                gitlabProject.path("name").asText(), gitlabProject.path("description").asText(null),
                gitlabProject.path("web_url").asText(null), createdAt);

        Set<Technology> technologies = new HashSet<>();
        Technology mainLanguage = null;
        for (var detected : technologyDetector.fromLanguages(apiClient.languages(baseUrl, token, projectId))) {
            Technology technology = technologyService.findOrCreate(detected.name(), detected.category());
            technologies.add(technology);
            if (mainLanguage == null && detected.category() == TechnologyCategory.LANGUAGE) {
                mainLanguage = technology;
            }
        }
        technologyDetector.fromRootFiles(apiClient.rootFiles(baseUrl, token, projectId)).forEach(detected ->
                technologies.add(technologyService.findOrCreate(detected.name(), detected.category())));
        if (!technologies.isEmpty()) {
            Instant moment = gitlabProject.hasNonNull("last_activity_at")
                    ? instant(gitlabProject.path("last_activity_at").asText()) : Instant.now();
            projectService.attachTechnologies(project.getId(), technologies);
            technologies.forEach(technology -> technologyService.registerUsage(user, technology, moment));
        }
        return new ImportedProject(project, mainLanguage);
    }

    private int importCreated(User user, String baseUrl, String token, String resource, ActivityType type,
                              String externalPrefix, Instant since, Map<Long, Project> projectsById) {
        int created = 0;
        for (JsonNode item : apiClient.createdByMe(baseUrl, token, resource, since)) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("iid", item.path("iid").asLong());
            metadata.put("url", item.path("web_url").asText());
            metadata.put("state", item.path("state").asText());
            if (activityService.record(user, type, ActivitySource.GITLAB, item.path("title").asText(), null,
                    instant(item.path("created_at").asText()), projectsById.get(item.path("project_id").asLong()),
                    null, externalPrefix + item.path("id").asLong(), metadata) != null) {
                created++;
            }
        }
        return created;
    }

    /** Commits nao trazem o id do autor no GitLab: casamos por e-mail ou nome. */
    static Set<String> identities(JsonNode me) {
        Set<String> identities = new HashSet<>();
        for (String field : List.of("email", "commit_email", "public_email", "name", "username")) {
            String value = me.path(field).asText("");
            if (!value.isBlank()) {
                identities.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return identities;
    }

    static boolean isMine(JsonNode commit, Set<String> identities) {
        return identities.contains(commit.path("author_email").asText("").toLowerCase(Locale.ROOT))
                || identities.contains(commit.path("author_name").asText("").toLowerCase(Locale.ROOT));
    }

    private static Instant instant(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }
}
