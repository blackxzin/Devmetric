package com.devmetrics.scoring.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateScoringRuleRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal basePoints,
        @Min(1) @Max(100) Integer dailyCap,
        Boolean diminishing,
        Boolean active
) {
}
