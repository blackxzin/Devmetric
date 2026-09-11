package com.devmetrics.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GitHubSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(GitHubSyncScheduler.class);

    private final GitHubSyncService syncService;
    private final GitHubSyncRunner syncRunner;

    public GitHubSyncScheduler(GitHubSyncService syncService, GitHubSyncRunner syncRunner) {
        this.syncService = syncService;
        this.syncRunner = syncRunner;
    }

    /** A cada 6 horas. Contas sincronizadas ha pouco sao ignoradas pelo proprio enqueue. */
    @Scheduled(cron = "0 0 */6 * * *")
    public void syncConnectedAccounts() {
        List<Long> syncIds = syncService.enqueueAllConnected();
        log.info("Sync automatico do GitHub: {} contas na fila", syncIds.size());
        syncIds.forEach(syncRunner::run);
    }
}
