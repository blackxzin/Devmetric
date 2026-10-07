package com.devmetrics.auth;

import com.devmetrics.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "chave-de-teste-com-mais-de-32-caracteres-ok";

    private final JwtService jwtService = new JwtService(properties(SECRET, 15));

    private AppProperties properties(String secret, int accessTtlMinutes) {
        return new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:3000")),
                "http://localhost:3000",
                new AppProperties.Jwt(secret, accessTtlMinutes, 7),
                new AppProperties.Security("0123456789abcdef0123456789abcdef"),
                new AppProperties.GitHub("", "", "", "https://api.github.com",
                        "https://github.com", 90, 60, 200, 10),
                new AppProperties.Scoring(90, 0.5));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(42L, "dev@devmetrics.com", "Dev", "America/Sao_Paulo", "USER");
    }

    @Test
    @DisplayName("token gerado volta a ser lido com os mesmos dados")
    void roundTrip() {
        String token = jwtService.generateAccessToken(user());

        Optional<AuthenticatedUser> parsed = jwtService.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(42L);
        assertThat(parsed.get().email()).isEqualTo("dev@devmetrics.com");
        assertThat(parsed.get().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("token adulterado e rejeitado")
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user());
        String tampered = token.substring(0, token.length() - 4) + "aaaa";

        assertThat(jwtService.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    @DisplayName("token assinado com outra chave e rejeitado")
    void tokenFromAnotherSecretIsRejected() {
        JwtService other = new JwtService(properties("outra-chave-de-teste-com-32-caracteres!!", 15));
        String token = other.generateAccessToken(user());

        assertThat(jwtService.parseAccessToken(token)).isEmpty();
    }

    @Test
    @DisplayName("token expirado e rejeitado")
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLived = new JwtService(properties(SECRET, 0));
        String token = shortLived.generateAccessToken(user());
        Thread.sleep(1100);

        assertThat(shortLived.parseAccessToken(token)).isEmpty();
    }

    @Test
    @DisplayName("lixo no lugar do token nao quebra a aplicacao")
    void garbageTokenIsRejected() {
        assertThat(jwtService.parseAccessToken("nao-e-um-jwt")).isEmpty();
        assertThat(jwtService.parseAccessToken("")).isEmpty();
    }

    @Test
    @DisplayName("refresh token e aleatorio e so o hash sai daqui")
    void refreshTokenIsRandomAndHashed() {
        String first = jwtService.generateRefreshToken();
        String second = jwtService.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(jwtService.hashRefreshToken(first))
                .hasSize(64)
                .isEqualTo(jwtService.hashRefreshToken(first))
                .isNotEqualTo(jwtService.hashRefreshToken(second));
    }
}
