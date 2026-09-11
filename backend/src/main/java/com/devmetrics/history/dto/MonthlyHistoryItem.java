package com.devmetrics.history.dto;

import java.math.BigDecimal;

public record MonthlyHistoryItem(
        int month,
        String label,
        long activities,
        BigDecimal points,
        int devScore,
        long distinctTypes,
        long distinctTechnologies,
        long newTechnologies,
        long newProjects,
        long challengesCompleted,
        int activeDays
) {
}
