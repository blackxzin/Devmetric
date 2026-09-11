package com.devmetrics.github.dto;

import java.time.Instant;

public record GitHubStatusResponse(
        boolean connected,
        boolean serverConfigured,
        String login,
        String avatarUrl,
        Instant connectedAt,
        Instant lastSyncedAt,
        Integer rateLimitRemaining
) {

    public static GitHubStatusResponse disconnected(boolean serverConfigured) {
        return new GitHubStatusResponse(false, serverConfigured, null, null, null, null, null);
    }
}
