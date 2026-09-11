package com.devmetrics.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuracao da aplicacao. Falha no startup se um segredo obrigatorio estiver ausente
 * ou fraco demais, em vez de falhar silenciosamente em producao.
 */
@Validated
@ConfigurationProperties(prefix = "devmetrics")
public record AppProperties(
        @Valid Cors cors,
        String frontendUrl,
        @Valid Jwt jwt,
        @Valid Security security,
        @Valid GitHub github,
        @Valid Scoring scoring
) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET precisa de no minimo 32 caracteres")
            String secret,
            @Positive int accessTtlMinutes,
            @Positive int refreshTtlDays
    ) {
    }

    public record Security(
            @NotBlank @Size(min = 32, max = 32, message = "TOKEN_ENCRYPTION_KEY precisa ter exatamente 32 caracteres")
            String tokenEncryptionKey
    ) {
    }

    public record GitHub(
            String clientId,
            String clientSecret,
            String redirectUri,
            String apiBaseUrl,
            String oauthBaseUrl,
            int firstSyncDays,
            int maxReposPerSync,
            int maxCommitDetailsPerSync,
            int minMinutesBetweenSyncs
    ) {
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }

    public record Scoring(int windowDays, double newTechnologyBonus) {
    }
}
