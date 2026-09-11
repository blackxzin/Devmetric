package com.devmetrics.achievement.dto;

import java.time.Instant;

public record AchievementResponse(
        String code,
        String name,
        String description,
        String icon,
        String category,
        int threshold,
        long progress,
        double progressRatio,
        boolean unlocked,
        Instant unlockedAt
) {
}
