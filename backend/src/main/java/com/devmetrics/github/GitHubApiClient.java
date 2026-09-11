package com.devmetrics.github;

import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.config.AppProperties;
import com.devmetrics.github.dto.GitHubCommitDetailDto;
import com.devmetrics.github.dto.GitHubCommitDto;
import com.devmetrics.github.dto.GitHubContentDto;
import com.devmetrics.github.dto.GitHubRepoDto;
import com.devmetrics.github.dto.GitHubSearchResponse;
import com.devmetrics.github.dto.GitHubUserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente REST do GitHub.
 *
 * Responsabilidades: montar a URL, mandar o token, paginar, acompanhar o rate limit
 * e traduzir erro HTTP em erro de dominio. Nao conhece atividade nem pontuacao.
 */
@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final String ACCEPT = "application/vnd.github+json";
    private static final String API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String API_VERSION = "2022-11-28";
    private static final int PER_PAGE = 100;
    private static final int MAX_RETRIES = 3;
    private static final int RATE_LIMIT_FLOOR = 100;

    private final RestClient restClient;
    private final AtomicInteger rateLimitRemaining = new AtomicInteger(-1);

    public GitHubApiClient(RestClient.Builder builder, AppProperties properties) {
        this.restClient = builder.baseUrl(properties.github().apiBaseUrl()).build();
    }

    public int rateLimitRemaining() {
        return rateLimitRemaining.get();
    }

    /** Abaixo deste piso o sync para e marca PARTIAL, em vez de tomar 403 no meio. */
    public boolean isNearRateLimit() {
        int remaining = rateLimitRemaining.get();
        return remaining >= 0 && remaining < RATE_LIMIT_FLOOR;
    }

    public GitHubUserDto currentUser(String token) {
        return get(token, "/user", new ParameterizedTypeReference<GitHubUserDto>() {
        });
    }

    public List<GitHubRepoDto> listRepositories(String token, int maxRepos) {
        List<GitHubRepoDto> repositories = new ArrayList<>();
        int page = 1;
        while (repositories.size() < maxRepos) {
            String uri = UriComponentsBuilder.fromPath("/user/repos")
                    .queryParam("affiliation", "owner,collaborator,organization_member")
                    .queryParam("sort", "pushed")
                    .queryParam("per_page", PER_PAGE)
                    .queryParam("page", page)
                    .toUriString();
            List<GitHubRepoDto> batch = get(token, uri, new ParameterizedTypeReference<List<GitHubRepoDto>>() {
            });
            if (batch == null || batch.isEmpty()) {
                break;
            }
            repositories.addAll(batch);
            if (batch.size() < PER_PAGE) {
                break;
            }
            page++;
        }
        return repositories.size() > maxRepos ? repositories.subList(0, maxRepos) : repositories;
    }

    public List<GitHubCommitDto> listCommits(String token, String owner, String repo,
                                             String author, Instant since) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/repos/{owner}/{repo}/commits")
                .queryParam("author", author)
                .queryParam("per_page", PER_PAGE);
        if (since != null) {
            builder.queryParam("since", since.toString());
        }
        String uri = builder.buildAndExpand(owner, repo).toUriString();
        List<GitHubCommitDto> commits = get(token, uri,
                new ParameterizedTypeReference<List<GitHubCommitDto>>() {
                });
        return commits == null ? List.of() : commits;
    }

    public GitHubCommitDetailDto commitDetail(String token, String owner, String repo, String sha) {
        String uri = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/commits/{sha}")
                .buildAndExpand(owner, repo, sha).toUriString();
        return get(token, uri, new ParameterizedTypeReference<GitHubCommitDetailDto>() {
        });
    }

    public Map<String, Long> languages(String token, String owner, String repo) {
        String uri = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/languages")
                .buildAndExpand(owner, repo).toUriString();
        Map<String, Long> languages = get(token, uri,
                new ParameterizedTypeReference<Map<String, Long>>() {
                });
        return languages == null ? Map.of() : languages;
    }

    public List<GitHubContentDto> rootContents(String token, String owner, String repo) {
        String uri = UriComponentsBuilder.fromPath("/repos/{owner}/{repo}/contents")
                .buildAndExpand(owner, repo).toUriString();
        List<GitHubContentDto> contents = get(token, uri,
                new ParameterizedTypeReference<List<GitHubContentDto>>() {
                });
        return contents == null ? List.of() : contents;
    }

    public GitHubSearchResponse searchIssues(String token, String query) {
        String uri = UriComponentsBuilder.fromPath("/search/issues")
                .queryParam("q", query)
                .queryParam("per_page", PER_PAGE)
                .queryParam("sort", "created")
                .queryParam("order", "desc")
                .build()
                .toUriString();
        return get(token, uri, new ParameterizedTypeReference<GitHubSearchResponse>() {
        });
    }

    // ---------------------------------------------------------------- interno

    private <T> T get(String token, String uri, ParameterizedTypeReference<T> type) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<T> response = restClient.get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(HttpHeaders.ACCEPT, ACCEPT)
                        .header(API_VERSION_HEADER, API_VERSION)
                        .retrieve()
                        .toEntity(type);
                readRateLimit(response.getHeaders());
                return response.getBody();
            } catch (HttpClientErrorException ex) {
                readRateLimit(ex.getResponseHeaders());
                handleClientError(ex);
                return null;
            } catch (HttpServerErrorException | ResourceAccessException ex) {
                // 5xx e timeout sao transitorios: vale repetir com backoff.
                lastError = ex;
                sleepBackoff(attempt);
            }
        }
        log.warn("GitHub indisponivel apos {} tentativas: {}", MAX_RETRIES,
                lastError == null ? "sem detalhe" : lastError.getMessage());
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "GitHub indisponivel no momento");
    }

    private void handleClientError(HttpClientErrorException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.value() == 401) {
            throw new BusinessException(ErrorCode.GITHUB_OAUTH_ERROR,
                    "Token do GitHub invalido ou revogado. Reconecte a conta.");
        }
        if (status.value() == 403 || status.value() == 429) {
            if (rateLimitRemaining.get() == 0 || isNearRateLimit()) {
                throw new BusinessException(ErrorCode.GITHUB_RATE_LIMITED);
            }
            throw new BusinessException(ErrorCode.GITHUB_OAUTH_ERROR,
                    "Acesso negado pelo GitHub. Verifique os escopos concedidos.");
        }
        if (status.value() == 404 || status.value() == 409) {
            // Repositorio vazio ou sem acesso: ignora e segue o sync.
            log.debug("Recurso do GitHub indisponivel: {}", status);
            return;
        }
        throw new BusinessException(ErrorCode.GITHUB_OAUTH_ERROR,
                "Falha ao consultar o GitHub (HTTP " + status.value() + ")");
    }

    private void readRateLimit(HttpHeaders headers) {
        if (headers == null) {
            return;
        }
        String remaining = headers.getFirst("x-ratelimit-remaining");
        if (remaining != null) {
            try {
                rateLimitRemaining.set(Integer.parseInt(remaining));
            } catch (NumberFormatException ignored) {
                // header fora do formato esperado: nao e motivo para abortar o sync
            }
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
