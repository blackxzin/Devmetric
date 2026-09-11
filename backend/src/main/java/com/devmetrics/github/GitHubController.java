package com.devmetrics.github;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.github.domain.SyncLog;
import com.devmetrics.github.dto.AuthorizeUrlResponse;
import com.devmetrics.github.dto.GitHubStatusResponse;
import com.devmetrics.github.dto.SyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/github")
@Tag(name = "GitHub", description = "Conexao OAuth e sincronizacao de atividade")
public class GitHubController {

    private final GitHubOAuthService oAuthService;
    private final GitHubSyncService syncService;
    private final GitHubSyncRunner syncRunner;

    public GitHubController(GitHubOAuthService oAuthService,
                            GitHubSyncService syncService,
                            GitHubSyncRunner syncRunner) {
        this.oAuthService = oAuthService;
        this.syncService = syncService;
        this.syncRunner = syncRunner;
    }

    @GetMapping("/authorize-url")
    @Operation(summary = "URL de autorizacao do GitHub para o usuario autenticado")
    public ApiResponse<AuthorizeUrlResponse> authorizeUrl(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(oAuthService.authorizeUrl(principal.id()));
    }

    @GetMapping("/callback")
    @Operation(summary = "Callback do OAuth. Redireciona de volta para o frontend.")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        String redirect = oAuthService.handleCallback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }

    @GetMapping("/status")
    @Operation(summary = "Estado da conexao com o GitHub")
    public ApiResponse<GitHubStatusResponse> status(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(syncService.status(principal.id()));
    }

    @PostMapping("/sync")
    @Operation(summary = "Dispara a sincronizacao em segundo plano")
    public ResponseEntity<ApiResponse<SyncResponse>> sync(@AuthenticationPrincipal AuthenticatedUser principal) {
        SyncLog syncLog = syncService.enqueue(principal.id());
        syncRunner.run(syncLog.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(SyncResponse.from(syncLog)));
    }

    @GetMapping("/sync/{syncId}")
    @Operation(summary = "Estado de uma sincronizacao")
    public ApiResponse<SyncResponse> syncStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable Long syncId) {
        return ApiResponse.ok(syncService.syncStatus(principal.id(), syncId));
    }

    @GetMapping("/sync")
    @Operation(summary = "Ultimas sincronizacoes do usuario")
    public ApiResponse<List<SyncResponse>> recentSyncs(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(syncService.recentSyncs(principal.id()));
    }

    @DeleteMapping("/disconnect")
    @Operation(summary = "Remove o token do GitHub. As atividades importadas permanecem.")
    public ApiResponse<Void> disconnect(@AuthenticationPrincipal AuthenticatedUser principal) {
        oAuthService.disconnect(principal.id());
        return ApiResponse.ok();
    }
}
