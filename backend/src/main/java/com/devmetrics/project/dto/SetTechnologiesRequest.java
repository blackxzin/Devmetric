package com.devmetrics.project.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetTechnologiesRequest(@NotNull List<Long> technologyIds) {
}
