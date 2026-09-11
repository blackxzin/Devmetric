package com.devmetrics.github;

import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.config.AppProperties;
import com.devmetrics.github.domain.GitHubAccount;
import com.devmetrics.github.dto.AuthorizeUrlResponse;
import com.devmetrics.github.dto.GitHubTokenResponse;
import com.devmetrics.github.dto.GitHubUserDto;
import com.devmetrics.github.repository.GitHubAccountRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fluxo OAuth Authorization Code do GitHub.
 *
 * Importante: isto NAO e login. E autorizacao de leitura. A conta do DevMetrics
 * continua existindo mesmo que o usuario revogue o acesso no GitHub.
 */
@Service
public class GitHubOAuthService {

    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final String SCOPES = "read:user,repo";

    private record PendingState(Long userId, Instant createdAt) {
    }

    private final Map<String, PendingState> pendingStates = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final AppProperties properties;
    private final RestClient restClient;
    private final GitHubApiClient gitHubApiClient;
    private final GitHubAccountRepository gitHubAccountRepository;
    private final UserRepository userRepository;
    private final TokenCipher tokenCipher;

    public GitHubOAuthService(AppProperties properties,
                              RestClient.Builder restClientBuilder,
                              GitHubApiClient gitHubApiClient,
                              GitHubAccountRepository gitHubAccountRepository,
                              UserRepository userRepository,
                              TokenCipher tokenCipher) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.github().oauthBaseUrl()).build();
        this.gitHubApiClient = gitHubApiClient;
        this.gitHubAccountRepository = gitHubAccountRepository;
        this.userRepository = userRepository;
        this.tokenCipher = tokenCipher;
    }

    public AuthorizeUrlResponse authorizeUrl(Long userId) {
        requireConfigured();
        purgeExpiredStates();

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendingStates.put(state, new PendingState(userId, Instant.now()));

        String url = UriComponentsBuilder.fromUriString(properties.github().oauthBaseUrl())
                .path("/login/oauth/authorize")
                .queryParam("client_id", properties.github().clientId())
                .queryParam("redirect_uri", properties.github().redirectUri())
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .build()
                .toUriString();
        return new AuthorizeUrlResponse(url, state);
    }

    /**
     * @return URL do frontend para onde o navegador deve ser redirecionado.
     */
    @Transactional
    public String handleCallback(String code, String state) {
        requireConfigured();
        purgeExpiredStates();

        PendingState pending = pendingStates.remove(state);
        if (pending == null) {
            // state desconhecido ou expirado: protege contra CSRF no callback
            throw new BusinessException(ErrorCode.GITHUB_OAUTH_ERROR, "State invalido ou expirado");
        }

        GitHubTokenResponse tokenResponse = exchangeCode(code);
        if (!tokenResponse.isValid()) {
            throw new BusinessException(ErrorCode.GITHUB_OAUTH_ERROR,
                    tokenResponse.errorDescription() == null
                            ? "O GitHub nao devolveu um token" : tokenResponse.errorDescription());
        }

        GitHubUserDto githubUser = gitHubApiClient.currentUser(tokenResponse.accessToken());
        if (githubUser == null || githubUser.id() == null) {
            throw new BusinessException(ErrorCode.GITHUB_OAUTH_ERROR, "Nao foi possivel ler o perfil do GitHub");
        }

        User user = userRepository.findById(pending.userId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        String encrypted = tokenCipher.encrypt(tokenResponse.accessToken());

        gitHubAccountRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        account -> account.refreshToken(encrypted, tokenResponse.scope()),
                        () -> gitHubAccountRepository.save(GitHubAccount.connect(user, githubUser.id(),
                                githubUser.login(), githubUser.avatarUrl(), encrypted, tokenResponse.scope())));

        if (user.getAvatarUrl() == null && githubUser.avatarUrl() != null) {
            user.updateAvatar(githubUser.avatarUrl());
        }

        return properties.frontendUrl() + "/dashboard.html?github=connected";
    }

    @Transactional
    public void disconnect(Long userId) {
        GitHubAccount account = gitHubAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));
        // As atividades ja importadas permanecem: sao o historico do usuario.
        gitHubAccountRepository.delete(account);
    }

    private GitHubTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.github().clientId());
        form.add("client_secret", properties.github().clientSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.github().redirectUri());

        return restClient.post()
                .uri("/login/oauth/access_token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GitHubTokenResponse.class);
    }

    private void requireConfigured() {
        if (!properties.github().isConfigured()) {
            throw new BusinessException(ErrorCode.GITHUB_NOT_CONFIGURED,
                    "Defina GITHUB_CLIENT_ID e GITHUB_CLIENT_SECRET no servidor");
        }
    }

    private void purgeExpiredStates() {
        Instant limit = Instant.now().minus(STATE_TTL);
        pendingStates.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(limit));
    }
}
