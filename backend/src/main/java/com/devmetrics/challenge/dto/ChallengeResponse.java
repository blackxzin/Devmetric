package com.devmetrics.challenge.dto;

import com.devmetrics.challenge.domain.DailyChallenge;

import java.time.Instant;
import java.time.LocalDate;

public record ChallengeResponse(
        Long id,
        LocalDate date,
        String title,
        String text,
        String activityType,
        String activityTypeLabel,
        int estimatedMinutes,
        String difficulty,
        String trigger,
        String status,
        Instant completedAt
) {

    public static ChallengeResponse from(DailyChallenge challenge) {
        return new ChallengeResponse(
                challenge.getId(),
                challenge.getChallengeDate(),
                challenge.getTemplate().getTitle(),
                challenge.getRenderedText(),
                challenge.getTemplate().getActivityType().name(),
                challenge.getTemplate().getActivityType().label(),
                challenge.getTemplate().getEstimatedMinutes(),
                challenge.getTemplate().getDifficulty().name(),
                challenge.getTemplate().getTrigger().name(),
                challenge.getStatus().name(),
                challenge.getCompletedAt());
    }
}
