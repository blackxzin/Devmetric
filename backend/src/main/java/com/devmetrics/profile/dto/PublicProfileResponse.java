package com.devmetrics.profile.dto;

import com.devmetrics.achievement.dto.AchievementResponse;
import com.devmetrics.dashboard.dto.CalendarResponse;
import com.devmetrics.dashboard.dto.StreakInfo;
import com.devmetrics.dashboard.dto.TopTechnology;
import com.devmetrics.scoring.dto.ScoreResponse;

import java.time.Instant;
import java.util.List;

/**
 * O que o mundo ve em /u/{username}. Nunca inclui e-mail, metadados de atividade
 * nem nada vindo do GitHub alem de agregados.
 */
public record PublicProfileResponse(
        String username,
        String displayName,
        String avatarUrl,
        Instant memberSince,
        ScoreResponse score,
        StreakInfo streak,
        long totalActivities,
        long totalProjects,
        List<TopTechnology> topTechnologies,
        List<AchievementResponse> achievements,
        CalendarResponse calendar
) {
}
