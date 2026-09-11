package com.devmetrics.dashboard.dto;

import java.time.Instant;

public record TopTechnology(String name, String category, int usageCount, Instant firstUsedAt, boolean isNew) {
}
