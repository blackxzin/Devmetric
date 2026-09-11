package com.devmetrics.github.domain;

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
@Table(name = "github_accounts")
public class GitHubAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "github_user_id", nullable = false)
    private Long githubUserId;

    @Column(name = "github_login", nullable = false, length = 100)
    private String githubLogin;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Token cifrado com AES-GCM. Nunca e exposto em nenhum DTO. */
    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "text")
    private String accessTokenEncrypted;

    @Column(length = 200)
    private String scopes;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected GitHubAccount() {
    }

    public static GitHubAccount connect(User user, Long githubUserId, String githubLogin,
                                        String avatarUrl, String accessTokenEncrypted, String scopes) {
        GitHubAccount account = new GitHubAccount();
        account.user = user;
        account.githubUserId = githubUserId;
        account.githubLogin = githubLogin;
        account.avatarUrl = avatarUrl;
        account.accessTokenEncrypted = accessTokenEncrypted;
        account.scopes = scopes;
        account.connectedAt = Instant.now();
        return account;
    }

    public void refreshToken(String accessTokenEncrypted, String scopes) {
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.scopes = scopes;
    }

    public void markSynced(Instant moment) {
        this.lastSyncedAt = moment;
    }

    public User getUser() {
        return user;
    }

    public Long getGithubUserId() {
        return githubUserId;
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public String getScopes() {
        return scopes;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }
}
