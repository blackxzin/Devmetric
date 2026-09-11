package com.devmetrics.user.dto;

public record UserProfileResponse(UserResponse user, boolean githubConnected, String githubLogin) {
}
