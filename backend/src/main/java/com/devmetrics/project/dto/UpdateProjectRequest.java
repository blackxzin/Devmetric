package com.devmetrics.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 140) String name,
        @Size(max = 2000) String description,
        LocalDate startedAt,
        @Size(max = 500) String repoUrl
) {
}
