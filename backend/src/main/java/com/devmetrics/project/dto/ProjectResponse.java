package com.devmetrics.project.dto;

import com.devmetrics.project.domain.Project;
import com.devmetrics.technology.dto.TechnologyResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String source,
        String repoUrl,
        LocalDate startedAt,
        LocalDate archivedAt,
        boolean archived,
        boolean readOnly,
        List<TechnologyResponse> technologies,
        Long activityCount,
        BigDecimal points
) {

    public static ProjectResponse from(Project project) {
        return from(project, null, null);
    }

    public static ProjectResponse from(Project project, Long activityCount, BigDecimal points) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getSource().name(),
                project.getRepoUrl(),
                project.getStartedAt(),
                project.getArchivedAt(),
                project.isArchived(),
                project.isReadOnly(),
                project.getTechnologies().stream()
                        .map(TechnologyResponse::from)
                        .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                        .toList(),
                activityCount,
                points);
    }
}
