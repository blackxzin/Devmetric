package com.devmetrics.gitlab;

import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Cliente REST v4 do GitLab. Respostas lidas como JsonNode: so usamos meia duzia
 * de campos, um DTO por endpoint seria mais codigo que logica.
 *
 * A URL base vem da conta (gitlab.com ou self-hosted), entao cada chamada recebe
 * baseUrl e token em vez de um cliente fixo.
 */
@Component
public class GitLabApiClient {

    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 5;

    private final RestClient restClient;

    public GitLabApiClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public JsonNode currentUser(String baseUrl, String token) {
        try {
            return get(baseUrl, token, "/user");
        } catch (HttpClientErrorException ex) {
            throw new BusinessException(ErrorCode.GITLAB_INVALID_TOKEN);
        }
    }

    public List<JsonNode> projects(String baseUrl, String token, Instant activeSince, int max) {
        return paged(baseUrl, token, "/projects", max, builder -> builder
                .queryParam("membership", true)
                .queryParam("order_by", "last_activity_at")
                .queryParam("last_activity_after", activeSince.toString()));
    }

    public List<JsonNode> commits(String baseUrl, String token, long projectId, Instant since) {
        return paged(baseUrl, token, "/projects/" + projectId + "/repository/commits", PER_PAGE * MAX_PAGES,
                builder -> builder.queryParam("since", since.toString()).queryParam("all", true));
    }

    public List<String> commitFiles(String baseUrl, String token, long projectId, String sha) {
        List<String> files = new ArrayList<>();
        JsonNode diff = getQuietly(baseUrl, token, "/projects/" + projectId + "/repository/commits/" + sha + "/diff");
        if (diff != null) {
            diff.forEach(node -> files.add(node.path("new_path").asText()));
        }
        return files;
    }

    public Map<String, Long> languages(String baseUrl, String token, long projectId) {
        JsonNode node = getQuietly(baseUrl, token, "/projects/" + projectId + "/languages");
        Map<String, Long> languages = new java.util.LinkedHashMap<>(); // ordem do GitLab: maior fatia primeiro
        if (node != null) {
            // GitLab devolve porcentagem; o detector so usa a ordem e o peso relativo.
            node.fields().forEachRemaining(entry -> languages.put(entry.getKey(),
                    Math.round(entry.getValue().asDouble() * 100)));
        }
        return languages;
    }

    public List<String> rootFiles(String baseUrl, String token, long projectId) {
        List<String> names = new ArrayList<>();
        JsonNode tree = getQuietly(baseUrl, token,
                "/projects/" + projectId + "/repository/tree?per_page=" + PER_PAGE);
        if (tree != null) {
            tree.forEach(node -> names.add(node.path("name").asText()));
        }
        return names;
    }

    public List<JsonNode> createdByMe(String baseUrl, String token, String resource, Instant since) {
        return paged(baseUrl, token, "/" + resource, PER_PAGE * MAX_PAGES, builder -> builder
                .queryParam("scope", "created_by_me")
                .queryParam("created_after", since.toString()));
    }

    // ---------------------------------------------------------------- http

    private List<JsonNode> paged(String baseUrl, String token, String path, int max,
                                 Consumer<UriComponentsBuilder> query) {
        List<JsonNode> items = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES && items.size() < max; page++) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path)
                    .queryParam("per_page", PER_PAGE)
                    .queryParam("page", page);
            query.accept(builder);
            JsonNode batch = get(baseUrl, token, builder.toUriString());
            if (batch == null || !batch.isArray() || batch.isEmpty()) {
                break;
            }
            batch.forEach(items::add);
            if (batch.size() < PER_PAGE) {
                break;
            }
        }
        return items.size() > max ? items.subList(0, max) : items;
    }

    /** Para dados opcionais (linguagens, arvore, diff): falha vira "sem dado", nao derruba o sync. */
    private JsonNode getQuietly(String baseUrl, String token, String path) {
        try {
            return get(baseUrl, token, path);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private JsonNode get(String baseUrl, String token, String path) {
        return restClient.get()
                .uri(apiRoot(baseUrl) + path)
                .header("PRIVATE-TOKEN", token)
                .retrieve()
                .body(JsonNode.class);
    }

    static String apiRoot(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/api/v4";
    }
}
