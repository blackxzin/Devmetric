package com.devmetrics.challenge.domain;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "challenge_templates")
public class ChallengeTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(name = "description_template", nullable = false, columnDefinition = "text")
    private String descriptionTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private ChallengeTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(nullable = false)
    private boolean active;

    protected ChallengeTemplate() {
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public ChallengeTrigger getTrigger() {
        return trigger;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public boolean isActive() {
        return active;
    }
}
