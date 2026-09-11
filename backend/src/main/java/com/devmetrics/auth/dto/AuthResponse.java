package com.devmetrics.auth.dto;

import com.devmetrics.user.dto.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
}
