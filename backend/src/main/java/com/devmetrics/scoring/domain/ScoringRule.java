package com.devmetrics.scoring.domain;

import com.devmetrics.activity.domain.ActivityType;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Regra de pontuacao de um tipo de atividade.
 * user = null significa regra padrao global; uma regra com user sobrescreve a global.
 */
@Entity
@Table(name = "scoring_rules",
        uniqueConstraints = @UniqueConstraint(name = "uk_scoring_rule",
                columnNames = {"user_id", "activity_type"}))
public class ScoringRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "base_points", nullable = false, precision = 6, scale = 2)
    private BigDecimal basePoints;

    @Column(name = "daily_cap")
    private Integer dailyCap;

    @Column(nullable = false)
    private boolean diminishing;

    @Column(nullable = false)
    private boolean active;

    protected ScoringRule() {
    }

    public static ScoringRule override(User user, ActivityType activityType, BigDecimal basePoints,
                                       Integer dailyCap, boolean diminishing, boolean active) {
        ScoringRule rule = new ScoringRule();
        rule.user = user;
        rule.activityType = activityType;
        rule.basePoints = basePoints;
        rule.dailyCap = dailyCap;
        rule.diminishing = diminishing;
        rule.active = active;
        return rule;
    }

    public void update(BigDecimal basePoints, Integer dailyCap, Boolean diminishing, Boolean active) {
        if (basePoints != null) {
            this.basePoints = basePoints;
        }
        this.dailyCap = dailyCap;
        if (diminishing != null) {
            this.diminishing = diminishing;
        }
        if (active != null) {
            this.active = active;
        }
    }

    public EffectiveRule toEffectiveRule() {
        return new EffectiveRule(activityType, basePoints, dailyCap, diminishing, active, user != null);
    }

    public User getUser() {
        return user;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public BigDecimal getBasePoints() {
        return basePoints;
    }

    public Integer getDailyCap() {
        return dailyCap;
    }

    public boolean isDiminishing() {
        return diminishing;
    }

    public boolean isActive() {
        return active;
    }
}
