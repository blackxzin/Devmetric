package com.devmetrics.challenge.domain;

import com.devmetrics.activity.domain.Activity;
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

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_challenges",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_challenge",
                columnNames = {"user_id", "challenge_date"}))
public class DailyChallenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ChallengeTemplate template;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @Column(name = "rendered_text", nullable = false, columnDefinition = "text")
    private String renderedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    protected DailyChallenge() {
    }

    public static DailyChallenge create(User user, ChallengeTemplate template,
                                        LocalDate challengeDate, String renderedText) {
        DailyChallenge challenge = new DailyChallenge();
        challenge.user = user;
        challenge.template = template;
        challenge.challengeDate = challengeDate;
        challenge.renderedText = renderedText;
        challenge.status = ChallengeStatus.PENDING;
        return challenge;
    }

    public void complete(Activity activity) {
        this.status = ChallengeStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.activity = activity;
    }

    public void skip(ChallengeTemplate newTemplate, String newText) {
        this.template = newTemplate;
        this.renderedText = newText;
        this.status = ChallengeStatus.PENDING;
    }

    public void expire() {
        this.status = ChallengeStatus.EXPIRED;
    }

    public User getUser() {
        return user;
    }

    public ChallengeTemplate getTemplate() {
        return template;
    }

    public LocalDate getChallengeDate() {
        return challengeDate;
    }

    public String getRenderedText() {
        return renderedText;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Activity getActivity() {
        return activity;
    }
}
