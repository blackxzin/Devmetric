package com.devmetrics.dashboard.dto;

import com.devmetrics.activity.dto.ActivityResponse;
import com.devmetrics.scoring.dto.ScoreResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummary(
        ScoreResponse score,
        StreakInfo streak,
        WeekSummary week,
        Totals totals,
        List<TopTechnology> topTechnologies,
        List<ActivityBreakdownItem> breakdown,
        List<ActivityResponse> recentActivities
) {

    public record WeekSummary(long activities, BigDecimal points, int activeDays, int goalPoints,
                              double goalProgress) {
    }

    public record Totals(long activities, long projects, long activeProjects, long technologies) {
    }
}
