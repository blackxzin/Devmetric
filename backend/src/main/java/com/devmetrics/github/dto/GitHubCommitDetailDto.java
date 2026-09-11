package com.devmetrics.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitDetailDto(String sha, Stats stats, List<FileChange> files) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(int additions, int deletions, int total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileChange(String filename, String status, int additions, int deletions,
                             @JsonProperty("changes") int changes) {
    }

    public List<String> fileNames() {
        return files == null ? List.of() : files.stream().map(FileChange::filename).toList();
    }
}
