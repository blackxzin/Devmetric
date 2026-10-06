package com.devmetrics.gitlab;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Bean separado pelo mesmo motivo do GitHubSyncRunner: @Async so funciona via proxy. */
@Component
public class GitLabSyncRunner {

    private final GitLabService gitLabService;

    public GitLabSyncRunner(GitLabService gitLabService) {
        this.gitLabService = gitLabService;
    }

    @Async("githubSyncExecutor")
    public void run(Long accountId) {
        gitLabService.execute(accountId);
    }
}
