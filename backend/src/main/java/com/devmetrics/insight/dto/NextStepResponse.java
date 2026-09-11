package com.devmetrics.insight.dto;

import java.util.List;

public record NextStepResponse(Suggestion suggestion, BasedOn basedOn, List<Suggestion> alternatives) {

    public record Suggestion(
            String technology,
            String category,
            String reason,
            String firstStep,
            int estimatedMinutes
    ) {
    }

    public record BasedOn(List<String> dominantTechnologies, long projectCount, long technologyCount,
                          long categoryCount) {
    }
}
