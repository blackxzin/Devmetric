package com.devmetrics.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateProjectRequest(
        @NotBlank @Size(max = 140) String name,
        @Size(max = 2000) String description,
        LocalDate startedAt,
        @Size(max = 500) String repoUrl,
        List<Long> technologyIds
) {
}
