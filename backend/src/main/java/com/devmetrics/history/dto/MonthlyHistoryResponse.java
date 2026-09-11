package com.devmetrics.history.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyHistoryResponse(
        int year,
        long totalActivities,
        BigDecimal totalPoints,
        int bestScore,
        int currentScore,
        List<MonthlyHistoryItem> months
) {
}
