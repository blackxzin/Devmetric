package com.devmetrics.gitlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param token   Personal Access Token com escopo read_api
 * @param baseUrl opcional; padrao https://gitlab.com (aceita instancia self-hosted)
 */
public record ConnectGitLabRequest(
        @NotBlank @Size(max = 200) String token,
        @Size(max = 200) @Pattern(regexp = "^https://[^\s/]+(/[^\s]*)?$", message = "use uma URL https")
        String baseUrl
) {
}
