package com.devmetrics.scoring.domain;

import com.devmetrics.activity.domain.ActivityType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Todos os numeros que o Dev Score consome. Deixar isso explicito torna o
 * calculo uma funcao pura, testavel sem banco.
 */
public record ScoreInput(
        int windowDays,
        int weeklyGoalPoints,
        BigDecimal rawPoints,
        int activeDays,
        int currentStreak,
        Map<ActivityType, BigDecimal> pointsByType,
        long newTechnologies,
        long studyActivities,
        long newProjects,
        long distinctTechnologies,
        long distinctTechnologyCategories
) {

    public static ScoreInput empty(int windowDays, int weeklyGoalPoints) {
        return new ScoreInput(windowDays, weeklyGoalPoints, BigDecimal.ZERO, 0, 0,
                Map.of(), 0, 0, 0, 0, 0);
    }
}
