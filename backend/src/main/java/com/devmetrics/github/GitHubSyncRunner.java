package com.devmetrics.github;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executa o sync fora da thread HTTP.
 *
 * Fica em um bean separado de proposito: chamar um metodo @Async da mesma classe
 * nao passa pelo proxy do Spring e rodaria de forma sincrona sem aviso.
 */
@Component
public class GitHubSyncRunner {

    private final GitHubSyncService syncService;

    public GitHubSyncRunner(GitHubSyncService syncService) {
        this.syncService = syncService;
    }

    @Async("githubSyncExecutor")
    public void run(Long syncLogId) {
        syncService.execute(syncLogId);
    }
}
