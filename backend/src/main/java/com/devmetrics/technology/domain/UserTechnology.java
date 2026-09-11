package com.devmetrics.technology.domain;

import com.devmetrics.common.audit.BaseEntity;
import com.devmetrics.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Marca a primeira vez que um usuario usou uma tecnologia.
 * E esta linha que define o que conta como "tecnologia nova".
 */
@Entity
@Table(name = "user_technologies",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_technology",
                columnNames = {"user_id", "technology_id"}))
public class UserTechnology extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technology_id", nullable = false)
    private Technology technology;

    @Column(name = "first_used_at", nullable = false)
    private Instant firstUsedAt;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    protected UserTechnology() {
    }

    public static UserTechnology first(User user, Technology technology, Instant firstUsedAt) {
        UserTechnology userTechnology = new UserTechnology();
        userTechnology.user = user;
        userTechnology.technology = technology;
        userTechnology.firstUsedAt = firstUsedAt;
        userTechnology.usageCount = 1;
        return userTechnology;
    }

    public void registerUsage(Instant occurredAt) {
        this.usageCount++;
        if (occurredAt != null && occurredAt.isBefore(this.firstUsedAt)) {
            this.firstUsedAt = occurredAt;
        }
    }

    public User getUser() {
        return user;
    }

    public Technology getTechnology() {
        return technology;
    }

    public Instant getFirstUsedAt() {
        return firstUsedAt;
    }

    public int getUsageCount() {
        return usageCount;
    }
}
