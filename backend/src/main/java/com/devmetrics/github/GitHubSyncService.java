package com.devmetrics.github;

import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.common.event.ActivityRecordedEvent;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.activity.ActivityService;
import com.devmetrics.config.AppProperties;
import com.devmetrics.github.domain.GitHubAccount;
import com.devmetrics.github.domain.SyncLog;
import com.devmetrics.github.domain.SyncStatus;
import com.devmetrics.github.dto.GitHubCommitDetailDto;
import com.devmetrics.github.dto.GitHubCommitDto;
import com.devmetrics.github.dto.GitHubContentDto;
import com.devmetrics.github.dto.GitHubRepoDto;
import com.devmetrics.github.dto.GitHubSearchResponse;
import com.devmetrics.github.dto.GitHubStatusResponse;
import com.devmetrics.github.dto.SyncResponse;
import com.devmetrics.github.repository.GitHubAccountRepository;
import com.devmetrics.github.repository.SyncLogRepository;
import com.devmetrics.project.ProjectService;
import com.devmetrics.project.domain.Project;
import com.devmetrics.scoring.ScoreService;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orquestra a importacao de atividade do GitHub.
 *
 * Nao e transacional de ponta a ponta de proposito: uma sincronizacao faz dezenas de
 * chamadas HTTP e nao pode segurar uma conexao de banco durante todas elas. Cada
 * gravacao acontece na transacao curta do servico correspondente.
 */
@Service
public class GitHubSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitHubSyncService.class);

    private final GitHubAccountRepository gitHubAccountRepository;
    private final SyncLogRepository syncLogRepository;
    private final UserRepository userRepository;
    private final GitHubApiClient apiClient;
    private final ActivityClassifier classifier;
    private final TechnologyDetector technologyDetector;
    private final TechnologyService technologyService;
    private final ProjectService projectService;
    private final ActivityService activityService;
    private final ScoreService scoreService;
    private final TokenCipher tokenCipher;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties properties;

    public GitHubSyncService(GitHubAccountRepository gitHubAccountRepository,
                             SyncLogRepository syncLogRepository,
                             UserRepository userRepository,
                             GitHubApiClient apiClient,
                             ActivityClassifier classifier,
                             TechnologyDetector technologyDetector,
                             TechnologyService technologyService,
                             ProjectService projectService,
                             ActivityService activityService,
                             ScoreService scoreService,
                             TokenCipher tokenCipher,
                             ApplicationEventPublisher eventPublisher,
                             AppProperties properties) {
        this.gitHubAccountRepository = gitHubAccountRepository;
        this.syncLogRepository = syncLogRepository;
        this.userRepository = userRepository;
        this.apiClient = apiClient;
        this.classifier = classifier;
        this.technologyDetector = technologyDetector;
        this.technologyService = technologyService;
        this.projectService = projectService;
        this.activityService = activityService;
        this.scoreService = scoreService;
        this.tokenCipher = tokenCipher;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    // ---------------------------------------------------------------- status

    @Transactional(readOnly = true)
    public GitHubStatusResponse status(Long userId) {
        boolean configured = properties.github().isConfigured();
        return gitHubAccountRepository.findByUserId(userId)
                .map(account -> new GitHubStatusResponse(true, configured, account.getGithubLogin(),
                        account.getAvatarUrl(), account.getConnectedAt(), account.getLastSyncedAt(),
                        apiClient.rateLimitRemaining() < 0 ? null : apiClient.rateLimitRemaining()))
                .orElseGet(() -> GitHubStatusResponse.disconnected(configured));
    }

    @Transactional(readOnly = true)
    public SyncResponse syncStatus(Long userId, Long syncId) {
        return syncLogRepository.findByIdAndUserId(syncId, userId)
                .map(SyncResponse::from)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SYNC_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<SyncResponse> recentSyncs(Long userId) {
        return syncLogRepository.findTop10ByUserIdOrderByStartedAtDesc(userId).stream()
                .map(SyncResponse::from)
                .toList();
    }

    // ---------------------------------------------------------------- disparo

    @Transactional
    public SyncLog enqueue(Long userId) {
        GitHubAccount account = gitHubAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));

        Instant lastSync = account.getLastSyncedAt();
        int minMinutes = properties.github().minMinutesBetweenSyncs();
        if (lastSync != null && lastSync.isAfter(Instant.now().minus(Duration.ofMinutes(minMinutes)))) {
            throw new BusinessException(ErrorCode.SYNC_TOO_FREQUENT,
                    "Aguarde " + minMinutes + " minutos entre sincronizacoes");
        }
        return syncLogRepository.save(SyncLog.start(account.getUser()));
    }

    // ---------------------------------------------------------------- execucao

    public void execute(Long syncLogId) {
        SyncLog syncLog = syncLogRepository.findById(syncLogId).orElse(null);
        if (syncLog == null) {
            return;
        }
        Long userId = syncLog.getUser().getId();
        User user = userRepository.findById(userId).orElse(null);
        GitHubAccount account = gitHubAccountRepository.findByUserId(userId).orElse(null);
        if (user == null || account == null) {
            syncLog.fail("Conta do GitHub nao encontrada");
            syncLogRepository.save(syncLog);
            return;
        }

        int reposScanned = 0;
        int activitiesCreated = 0;
        boolean partial = false;

        try {
            String token = tokenCipher.decrypt(account.getAccessTokenEncrypted());
            Instant since = account.getLastSyncedAt() != null
                    ? account.getLastSyncedAt()
                    : Instant.now().minus(Duration.ofDays(properties.github().firstSyncDays()));

            List<GitHubRepoDto> repositories =
                    apiClient.listRepositories(token, properties.github().maxReposPerSync());

            Map<String, Project> projectsByFullName = new HashMap<>();
            int detailBudget = properties.github().maxCommitDetailsPerSync();

            for (GitHubRepoDto repository : repositories) {
                if (repository.fork() || repository.ownerLogin() == null) {
                    continue;
                }
                if (apiClient.isNearRateLimit()) {
                    partial = true;
                    break;
                }

                Project project = syncRepository(user, token, repository);
                projectsByFullName.put(repository.fullName(), project);
                reposScanned++;

                if (repository.pushedAt() != null && repository.pushedAt().isBefore(since)) {
                    continue;
                }

                Technology mainTechnology = repository.language() == null ? null
                        : technologyService.findOrCreate(repository.language(),
                        com.devmetrics.technology.domain.TechnologyCategory.LANGUAGE);

                List<GitHubCommitDto> commits = apiClient.listCommits(token,
                        repository.ownerLogin(), repository.name(), account.getGithubLogin(), since);

                for (GitHubCommitDto commit : commits) {
                    List<String> changedFiles = List.of();
                    Integer additions = null;
                    Integer deletions = null;

                    if (detailBudget > 0 && classifier.needsFileInspection(commit.message())) {
                        detailBudget--;
                        GitHubCommitDetailDto detail = apiClient.commitDetail(token,
                                repository.ownerLogin(), repository.name(), commit.sha());
                        if (detail != null) {
                            changedFiles = detail.fileNames();
                            if (detail.stats() != null) {
                                additions = detail.stats().additions();
                                deletions = detail.stats().deletions();
                            }
                        }
                    }

                    ActivityClassifier.Classification classification =
                            classifier.classify(commit.message(), changedFiles);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("repository", repository.fullName());
                    metadata.put("sha", commit.sha());
                    metadata.put("url", commit.htmlUrl());
                    metadata.put("classifiedBy", classification.classifiedBy());
                    if (additions != null) {
                        metadata.put("additions", additions);
                        metadata.put("deletions", deletions);
                    }

                    var created = activityService.record(user, classification.type(), ActivitySource.GITHUB,
                            firstLine(commit.message()), commit.message(), commit.committedAt(),
                            project, mainTechnology, "commit:" + commit.sha(), metadata);
                    if (created != null) {
                        activitiesCreated++;
                    }
                }
            }

            activitiesCreated += syncIssuesAndPullRequests(user, token, account.getGithubLogin(),
                    since, projectsByFullName);

            account.markSynced(Instant.now());
            gitHubAccountRepository.save(account);

            scoreService.snapshot(user, LocalDate.now(user.zoneId()));
            eventPublisher.publishEvent(new ActivityRecordedEvent(userId));

            syncLog.finish(partial ? SyncStatus.PARTIAL : SyncStatus.SUCCESS, reposScanned, activitiesCreated);
        } catch (BusinessException ex) {
            log.warn("Sync do GitHub falhou para o usuario {}: {}", userId, ex.getMessage());
            syncLog.fail(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Erro inesperado no sync do GitHub para o usuario {}", userId, ex);
            syncLog.fail("Erro inesperado durante a sincronizacao");
        }
        syncLogRepository.save(syncLog);
    }

    // ---------------------------------------------------------------- apoio

    private Project syncRepository(User user, String token, GitHubRepoDto repository) {
        LocalDate createdAt = repository.createdAt() == null
                ? LocalDate.now(user.zoneId())
                : repository.createdAt().atZone(ZoneOffset.UTC).toLocalDate();

        Project project = projectService.upsertFromGitHub(user, String.valueOf(repository.id()),
                repository.name(), repository.description(), repository.htmlUrl(), createdAt);

        Set<Technology> technologies = new HashSet<>();
        Instant moment = repository.pushedAt() == null ? Instant.now() : repository.pushedAt();

        for (var detected : technologyDetector.fromLanguages(
                apiClient.languages(token, repository.ownerLogin(), repository.name()))) {
            technologies.add(technologyService.findOrCreate(detected.name(), detected.category()));
        }

        List<GitHubContentDto> contents =
                apiClient.rootContents(token, repository.ownerLogin(), repository.name());
        List<String> fileNames = contents.stream().map(GitHubContentDto::name).toList();
        for (var detected : technologyDetector.fromRootFiles(fileNames)) {
            technologies.add(technologyService.findOrCreate(detected.name(), detected.category()));
        }

        if (!technologies.isEmpty()) {
            projectService.attachTechnologies(project.getId(), technologies);
            technologies.forEach(technology -> technologyService.registerUsage(user, technology, moment));
        }
        return project;
    }

    private int syncIssuesAndPullRequests(User user, String token, String login, Instant since,
                                          Map<String, Project> projectsByFullName) {
        String sinceDate = since.atZone(ZoneOffset.UTC).toLocalDate().toString();
        int created = 0;

        created += importSearch(user, token,
                "author:" + login + " type:pr created:>=" + sinceDate,
                ActivityType.PULL_REQUEST, "pr:", projectsByFullName);
        created += importSearch(user, token,
                "author:" + login + " type:issue created:>=" + sinceDate,
                ActivityType.ISSUE, "issue:", projectsByFullName);
        return created;
    }

    private int importSearch(User user, String token, String query, ActivityType type,
                             String externalPrefix, Map<String, Project> projectsByFullName) {
        GitHubSearchResponse response = apiClient.searchIssues(token, query);
        if (response == null || response.items() == null) {
            return 0;
        }
        int created = 0;
        for (GitHubSearchResponse.Item item : response.items()) {
            if (type == ActivityType.ISSUE && item.isPullRequest()) {
                continue;
            }
            Project project = projectsByFullName.get(item.repositoryFullName());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("repository", item.repositoryFullName());
            metadata.put("number", item.number());
            metadata.put("url", item.htmlUrl());
            metadata.put("state", item.state());

            var activity = activityService.record(user, type, ActivitySource.GITHUB, item.title(), null,
                    item.createdAt() == null ? Instant.now() : item.createdAt(),
                    project, null, externalPrefix + item.id(), metadata);
            if (activity != null) {
                created++;
            }
        }
        return created;
    }

    private String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "Commit";
        }
        return message.strip().lines().findFirst().orElse("Commit");
    }

    /**
     * Sincronizacao automatica a cada 6 horas para todas as contas conectadas.
     */
    public List<Long> enqueueAllConnected() {
        List<Long> enqueued = new ArrayList<>();
        for (GitHubAccount account : gitHubAccountRepository.findAllWithUser()) {
            try {
                enqueued.add(enqueue(account.getUser().getId()).getId());
            } catch (BusinessException ignored) {
                // conta sincronizada ha pouco: nada a fazer neste ciclo
            }
        }
        return enqueued;
    }
}
