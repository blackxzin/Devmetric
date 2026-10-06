package com.devmetrics.github;

import com.devmetrics.activity.ActivityService;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.common.event.ActivityRecordedEvent;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.config.AppProperties;
import com.devmetrics.github.domain.GitHubAccount;
import com.devmetrics.github.repository.GitHubAccountRepository;
import com.devmetrics.project.domain.Project;
import com.devmetrics.project.domain.ProjectSource;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recebe eventos "push" do GitHub e registra os commits na hora, sem esperar o sync.
 *
 * O payload do push ja traz mensagem e arquivos alterados de cada commit, entao a
 * classificacao e a mesma do sync sem nenhuma chamada extra a API. A chave externa
 * ("commit:sha") e a mesma do sync: um commit recebido aqui nunca e duplicado depois.
 */
@Service
public class GitHubWebhookService {

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final GitHubAccountRepository gitHubAccountRepository;
    private final ProjectRepository projectRepository;
    private final TechnologyService technologyService;
    private final ActivityService activityService;
    private final ActivityClassifier classifier;
    private final ApplicationEventPublisher eventPublisher;

    public GitHubWebhookService(AppProperties properties,
                                ObjectMapper objectMapper,
                                GitHubAccountRepository gitHubAccountRepository,
                                ProjectRepository projectRepository,
                                TechnologyService technologyService,
                                ActivityService activityService,
                                ActivityClassifier classifier,
                                ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.gitHubAccountRepository = gitHubAccountRepository;
        this.projectRepository = projectRepository;
        this.technologyService = technologyService;
        this.activityService = activityService;
        this.classifier = classifier;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @return quantidade de atividades criadas
     */
    @Transactional
    public int handle(String event, String signature, byte[] body) {
        verifySignature(signature, body);
        if (!"push".equals(event)) {
            return 0; // "ping" e qualquer outro evento: aceito e ignorado
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(body);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Payload invalido");
        }

        GitHubAccount account = gitHubAccountRepository
                .findByGithubUserId(payload.path("sender").path("id").asLong())
                .orElse(null);
        if (account == null) {
            return 0; // push de alguem que nao usa o DevMetrics
        }
        User user = account.getUser();
        JsonNode repository = payload.path("repository");

        Project project = projectRepository.findByUserIdAndSourceAndExternalId(
                user.getId(), ProjectSource.GITHUB, repository.path("id").asText()).orElse(null);
        String language = repository.path("language").asText(null);
        Technology technology = language == null ? null
                : technologyService.findOrCreate(language, TechnologyCategory.LANGUAGE);

        int created = 0;
        for (JsonNode commit : payload.path("commits")) {
            String author = commit.path("author").path("username").asText("");
            if (!author.equalsIgnoreCase(account.getGithubLogin()) || !commit.path("distinct").asBoolean(true)) {
                continue;
            }
            String message = commit.path("message").asText("");
            ActivityClassifier.Classification classification = classifier.classify(message, changedFiles(commit));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("repository", repository.path("full_name").asText());
            metadata.put("sha", commit.path("id").asText());
            metadata.put("url", commit.path("url").asText());
            metadata.put("classifiedBy", classification.classifiedBy());
            metadata.put("via", "webhook");

            var activity = activityService.record(user, classification.type(), ActivitySource.GITHUB,
                    firstLine(message), message, timestamp(commit), project, technology,
                    "commit:" + commit.path("id").asText(), metadata);
            if (activity != null) {
                created++;
            }
        }
        if (created > 0) {
            eventPublisher.publishEvent(new ActivityRecordedEvent(user.getId()));
        }
        return created;
    }

    void verifySignature(String signature, byte[] body) {
        String secret = properties.github().webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.GITHUB_NOT_CONFIGURED, "Defina GITHUB_WEBHOOK_SECRET no servidor");
        }
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new BusinessException(ErrorCode.INVALID_SIGNATURE);
        }
        byte[] expected = ("sha256=" + hmacSha256(secret, body)).getBytes(StandardCharsets.UTF_8);
        // Comparacao em tempo constante: nao deixa descobrir a assinatura byte a byte.
        if (!MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.INVALID_SIGNATURE);
        }
    }

    static String hmacSha256(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 indisponivel", ex);
        }
    }

    private List<String> changedFiles(JsonNode commit) {
        Set<String> files = new HashSet<>();
        for (String field : List.of("added", "modified", "removed")) {
            commit.path(field).forEach(node -> files.add(node.asText()));
        }
        return new ArrayList<>(files);
    }

    private Instant timestamp(JsonNode commit) {
        try {
            return OffsetDateTime.parse(commit.path("timestamp").asText()).toInstant();
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }

    private String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "Commit";
        }
        return message.strip().lines().findFirst().orElse("Commit");
    }
}
