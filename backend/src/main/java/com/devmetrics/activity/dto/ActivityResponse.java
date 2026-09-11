package com.devmetrics.activity.dto;

import com.devmetrics.activity.domain.Activity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record ActivityResponse(
        Long id,
        String type,
        String typeLabel,
        String source,
        String title,
        String description,
        Instant occurredAt,
        LocalDate activityDate,
        BigDecimal points,
        Long projectId,
        String projectName,
        Long technologyId,
        String technologyName,
        String externalId,
        Map<String, Object> metadata,
        boolean readOnly
) {

    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getType().name(),
                activity.getType().label(),
                activity.getSource().name(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getOccurredAt(),
                activity.getActivityDate(),
                activity.getPoints(),
                activity.getProject() == null ? null : activity.getProject().getId(),
                activity.getProject() == null ? null : activity.getProject().getName(),
                activity.getTechnology() == null ? null : activity.getTechnology().getId(),
                activity.getTechnology() == null ? null : activity.getTechnology().getName(),
                activity.getExternalId(),
                activity.getMetadata(),
                activity.isReadOnly());
    }
}
