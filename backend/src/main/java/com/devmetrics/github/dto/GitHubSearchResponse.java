package com.devmetrics.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubSearchResponse(
        @JsonProperty("total_count") int totalCount,
        List<Item> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long id,
            int number,
            String title,
            String state,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("repository_url") String repositoryUrl,
            @JsonProperty("pull_request") PullRequestRef pullRequest
    ) {

        public boolean isPullRequest() {
            return pullRequest != null;
        }

        /** Extrai "owner/repo" de https://api.github.com/repos/owner/repo */
        public String repositoryFullName() {
            if (repositoryUrl == null) {
                return null;
            }
            int index = repositoryUrl.indexOf("/repos/");
            return index < 0 ? null : repositoryUrl.substring(index + "/repos/".length());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequestRef(@JsonProperty("html_url") String htmlUrl) {
    }
}
