package com.devmetrics.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitDto(
        String sha,
        @JsonProperty("html_url") String htmlUrl,
        CommitPayload commit,
        Author author
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitPayload(String message, CommitAuthor author) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitAuthor(String name, String email, Instant date) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(String login) {
    }

    public String message() {
        return commit == null ? "" : commit.message();
    }

    public Instant committedAt() {
        if (commit == null || commit.author() == null || commit.author().date() == null) {
            return Instant.now();
        }
        return commit.author().date();
    }
}
