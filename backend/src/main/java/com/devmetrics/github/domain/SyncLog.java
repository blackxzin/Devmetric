package com.devmetrics.github.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sync_logs")
public class SyncLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncStatus status;

    @Column(name = "repos_scanned", nullable = false)
    private int reposScanned;

    @Column(name = "activities_created", nullable = false)
    private int activitiesCreated;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected SyncLog() {
    }

    public static SyncLog start(User user) {
        SyncLog log = new SyncLog();
        log.user = user;
        log.startedAt = Instant.now();
        log.status = SyncStatus.RUNNING;
        return log;
    }

    public void finish(SyncStatus status, int reposScanned, int activitiesCreated) {
        this.status = status;
        this.reposScanned = reposScanned;
        this.activitiesCreated = activitiesCreated;
        this.finishedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = SyncStatus.FAILED;
        this.errorMessage = errorMessage == null ? null
                : errorMessage.substring(0, Math.min(500, errorMessage.length()));
        this.finishedAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public int getReposScanned() {
        return reposScanned;
    }

    public int getActivitiesCreated() {
        return activitiesCreated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
