package com.devmetrics.github.dto;

import com.devmetrics.github.domain.SyncLog;

import java.time.Instant;

public record SyncResponse(
        Long syncId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        int reposScanned,
        int activitiesCreated,
        String errorMessage
) {

    public static SyncResponse from(SyncLog log) {
        return new SyncResponse(log.getId(), log.getStatus().name(), log.getStartedAt(),
                log.getFinishedAt(), log.getReposScanned(), log.getActivitiesCreated(),
                log.getErrorMessage());
    }
}
