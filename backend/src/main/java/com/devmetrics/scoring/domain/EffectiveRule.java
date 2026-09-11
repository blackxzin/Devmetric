package com.devmetrics.scoring.domain;

import com.devmetrics.activity.domain.ActivityType;

import java.math.BigDecimal;

/**
 * Regra ja resolvida (override do usuario ou padrao global), pronta para o calculo.
 * Objeto imutavel: o calculo nunca altera a regra.
 */
public record EffectiveRule(
        ActivityType activityType,
        BigDecimal basePoints,
        Integer dailyCap,
        boolean diminishing,
        boolean active,
        boolean customized
) {

    public static EffectiveRule disabled(ActivityType type) {
        return new EffectiveRule(type, BigDecimal.ZERO, 0, false, false, false);
    }
}
