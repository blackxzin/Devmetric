package com.devmetrics.scoring.dto;

import com.devmetrics.scoring.domain.ScoreComponent;

import java.time.Instant;
import java.util.List;

public record ScoreResponse(
        int devScore,
        String level,
        int windowDays,
        Instant calculatedAt,
        List<ScoreComponent> breakdown,
        Trend trend
) {

    public record Trend(int delta30d, String direction) {
    }
}
