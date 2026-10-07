package com.devmetrics.github;

import com.devmetrics.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TokenCipherTest {

    private final TokenCipher cipher = new TokenCipher(new AppProperties(
            new AppProperties.Cors(List.of("http://localhost:3000")),
            "http://localhost:3000",
            new AppProperties.Jwt("chave-de-teste-com-mais-de-32-caracteres-ok", 15, 7),
            new AppProperties.Security("devmetrics-chave-aes-local-32ch!!"),
            new AppProperties.GitHub("", "", "", "https://api.github.com",
                    "https://github.com", 90, 60, 200, 10),
            new AppProperties.Scoring(90, 0.5)));

    @Test
    void rejectsInvalidUtf8KeyLength() {
        AppProperties properties = mock(AppProperties.class);
        when(properties.security()).thenReturn(new AppProperties.Security("á".repeat(32)));
        assertThatThrownBy(() -> new TokenCipher(properties))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("o token cifrado volta ao original")
    void roundTrip() {
        String token = "gho_umTokenDeTesteQualquer123";

        String encrypted = cipher.encrypt(token);

        assertThat(encrypted).isNotEqualTo(token);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(token);
    }

    @Test
    @DisplayName("cifrar duas vezes o mesmo token gera textos diferentes")
    void encryptionIsNotDeterministic() {
        String token = "gho_umTokenDeTesteQualquer123";

        assertThat(cipher.encrypt(token)).isNotEqualTo(cipher.encrypt(token));
    }
}
