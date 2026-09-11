package com.devmetrics.activity.dto;

import com.devmetrics.activity.domain.ActivityType;

import java.util.List;

public record ActivityTypeInfo(String value, String label) {

    public static List<ActivityTypeInfo> all() {
        return List.of(ActivityType.values()).stream()
                .map(type -> new ActivityTypeInfo(type.name(), type.label()))
                .toList();
    }
}
