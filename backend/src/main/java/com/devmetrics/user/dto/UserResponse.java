package com.devmetrics.user.dto;

import com.devmetrics.user.domain.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        String avatarUrl,
        String timezone,
        int weeklyGoalPoints,
        String role,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl(),
                user.getTimezone(), user.getWeeklyGoalPoints(), user.getRole().name(), user.getCreatedAt());
    }
}
