package com.devmetrics.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUserDto(
        Long id,
        String login,
        String name,
        @JsonProperty("avatar_url") String avatarUrl
) {
}
