package com.devmetrics.gitlab.dto;

import com.devmetrics.gitlab.domain.GitLabAccount;

import java.time.Instant;

public record GitLabStatusResponse(
        boolean connected,
        String username,
        String avatarUrl,
        String baseUrl,
        Instant connectedAt,
        Instant lastSyncedAt,
        String lastSyncStatus,
        String lastSyncMessage,
        Integer lastSyncActivities
) {

    public static GitLabStatusResponse from(GitLabAccount account) {
        return new GitLabStatusResponse(true, account.getUsername(), account.getAvatarUrl(), account.getBaseUrl(),
                account.getConnectedAt(), account.getLastSyncedAt(), account.getLastSyncStatus(),
                account.getLastSyncMessage(), account.getLastSyncActivities());
    }

    public static GitLabStatusResponse disconnected() {
        return new GitLabStatusResponse(false, null, null, null, null, null, null, null, null);
    }
}
