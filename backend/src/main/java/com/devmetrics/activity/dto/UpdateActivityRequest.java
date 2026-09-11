package com.devmetrics.activity.dto;

import com.devmetrics.activity.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateActivityRequest(
        @NotNull ActivityType type,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        Long projectId,
        Long technologyId,
        Instant occurredAt
) {
}
