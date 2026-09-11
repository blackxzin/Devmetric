package com.devmetrics.scoring.dto;

import com.devmetrics.scoring.domain.EffectiveRule;

import java.math.BigDecimal;

public record ScoringRuleResponse(
        String activityType,
        String label,
        BigDecimal basePoints,
        Integer dailyCap,
        boolean diminishing,
        boolean active,
        boolean customized
) {

    public static ScoringRuleResponse from(EffectiveRule rule) {
        return new ScoringRuleResponse(rule.activityType().name(), rule.activityType().label(),
                rule.basePoints(), rule.dailyCap(), rule.diminishing(), rule.active(), rule.customized());
    }
}
