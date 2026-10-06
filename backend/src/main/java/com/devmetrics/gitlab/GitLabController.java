package com.devmetrics.gitlab;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.gitlab.dto.ConnectGitLabRequest;
import com.devmetrics.gitlab.dto.GitLabStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gitlab")
@Tag(name = "GitLab", description = "Conexao por token pessoal e sincronizacao de atividade")
public class GitLabController {

    private final GitLabService gitLabService;
    private final GitLabSyncRunner syncRunner;

    public GitLabController(GitLabService gitLabService, GitLabSyncRunner syncRunner) {
        this.gitLabService = gitLabService;
        this.syncRunner = syncRunner;
    }

    @GetMapping("/status")
    @Operation(summary = "Estado da conexao com o GitLab e do ultimo sync")
    public ApiResponse<GitLabStatusResponse> status(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(gitLabService.status(principal.id()));
    }

    @PostMapping("/connect")
    @Operation(summary = "Conecta com um Personal Access Token (escopo read_api)")
    public ApiResponse<GitLabStatusResponse> connect(@AuthenticationPrincipal AuthenticatedUser principal,
                                                     @Valid @RequestBody ConnectGitLabRequest request) {
        return ApiResponse.ok(gitLabService.connect(principal.id(), request));
    }

    @PostMapping("/sync")
    @Operation(summary = "Dispara a sincronizacao em segundo plano")
    public ResponseEntity<ApiResponse<GitLabStatusResponse>> sync(@AuthenticationPrincipal AuthenticatedUser principal) {
        Long accountId = gitLabService.markRunning(principal.id());
        syncRunner.run(accountId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(gitLabService.status(principal.id())));
    }

    @DeleteMapping("/disconnect")
    @Operation(summary = "Remove o token. As atividades importadas permanecem.")
    public ApiResponse<Void> disconnect(@AuthenticationPrincipal AuthenticatedUser principal) {
        gitLabService.disconnect(principal.id());
        return ApiResponse.ok();
    }
}
