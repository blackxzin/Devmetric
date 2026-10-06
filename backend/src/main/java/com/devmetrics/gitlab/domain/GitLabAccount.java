package com.devmetrics.gitlab.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "gitlab_accounts")
public class GitLabAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "gitlab_user_id", nullable = false)
    private Long gitlabUserId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "base_url", nullable = false, length = 200)
    private String baseUrl;

    /** Token cifrado com AES-GCM (mesmo TokenCipher do GitHub). Nunca sai em DTO. */
    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "text")
    private String accessTokenEncrypted;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_sync_status", length = 20)
    private String lastSyncStatus;

    @Column(name = "last_sync_message", length = 500)
    private String lastSyncMessage;

    @Column(name = "last_sync_activities")
    private Integer lastSyncActivities;

    protected GitLabAccount() {
    }

    public static GitLabAccount connect(User user, Long gitlabUserId, String username, String avatarUrl,
                                        String baseUrl, String accessTokenEncrypted) {
        GitLabAccount account = new GitLabAccount();
        account.user = user;
        account.gitlabUserId = gitlabUserId;
        account.username = username;
        account.avatarUrl = avatarUrl;
        account.baseUrl = baseUrl;
        account.accessTokenEncrypted = accessTokenEncrypted;
        account.connectedAt = Instant.now();
        return account;
    }

    public void markRunning() {
        this.lastSyncStatus = "RUNNING";
        this.lastSyncMessage = null;
    }

    public void markFinished(Instant moment, int activities) {
        this.lastSyncedAt = moment;
        this.lastSyncStatus = "SUCCESS";
        this.lastSyncActivities = activities;
        this.lastSyncMessage = null;
    }

    public void markFailed(String message) {
        this.lastSyncStatus = "FAILED";
        this.lastSyncMessage = message == null ? null : message.substring(0, Math.min(500, message.length()));
    }

    public User getUser() {
        return user;
    }

    public Long getGitlabUserId() {
        return gitlabUserId;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getLastSyncStatus() {
        return lastSyncStatus;
    }

    public String getLastSyncMessage() {
        return lastSyncMessage;
    }

    public Integer getLastSyncActivities() {
        return lastSyncActivities;
    }
}
