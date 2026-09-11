package com.devmetrics.achievement.domain;

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

@Entity
@Table(name = "user_achievements",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_achievement",
                columnNames = {"user_id", "achievement_id"}))
public class UserAchievement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    protected UserAchievement() {
    }

    public static UserAchievement unlock(User user, Achievement achievement) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.user = user;
        userAchievement.achievement = achievement;
        userAchievement.unlockedAt = Instant.now();
        return userAchievement;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }
}
