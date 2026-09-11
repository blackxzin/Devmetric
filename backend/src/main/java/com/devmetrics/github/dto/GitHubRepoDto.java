package com.devmetrics.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepoDto(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("pushed_at") Instant pushedAt,
        boolean fork,
        @JsonProperty("private") boolean isPrivate,
        String language,
        Owner owner
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {
    }

    public String ownerLogin() {
        return owner == null ? null : owner.login();
    }
}
