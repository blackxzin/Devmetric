package com.devmetrics.technology.dto;

import com.devmetrics.technology.domain.UserTechnology;

import java.time.Instant;

public record UserTechnologyResponse(
        Long technologyId,
        String name,
        String category,
        Instant firstUsedAt,
        int usageCount
) {

    public static UserTechnologyResponse from(UserTechnology userTechnology) {
        return new UserTechnologyResponse(
                userTechnology.getTechnology().getId(),
                userTechnology.getTechnology().getName(),
                userTechnology.getTechnology().getCategory().name(),
                userTechnology.getFirstUsedAt(),
                userTechnology.getUsageCount());
    }
}
