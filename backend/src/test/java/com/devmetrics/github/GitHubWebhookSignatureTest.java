package com.devmetrics.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubWebhookSignatureTest {

    @Test
    @DisplayName("HMAC-SHA256 bate com o exemplo da documentacao do GitHub")
    void matchesGitHubDocsExample() {
        // https://docs.github.com/webhooks/using-webhooks/validating-webhook-deliveries
        String signature = GitHubWebhookService.hmacSha256("It's a Secret to Everybody",
                "Hello, World!".getBytes(StandardCharsets.UTF_8));

        assertThat(signature).isEqualTo("757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17");
    }
}
