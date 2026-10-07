package com.devmetrics.auth;

import com.devmetrics.auth.domain.RefreshToken;
import com.devmetrics.auth.dto.LoginRequest;
import com.devmetrics.auth.dto.RegisterRequest;
import com.devmetrics.auth.repository.RefreshTokenRepository;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new com.devmetrics.config.AppProperties(
                new com.devmetrics.config.AppProperties.Cors(List.of("http://localhost:3000")),
                "http://localhost:3000",
                new com.devmetrics.config.AppProperties.Jwt(
                        "chave-de-teste-com-mais-de-32-caracteres-ok", 15, 7),
                new com.devmetrics.config.AppProperties.Security("0123456789abcdef0123456789abcdef"),
                new com.devmetrics.config.AppProperties.GitHub("", "", "", "https://api.github.com",
                        "https://github.com", 90, 60, 200, 10),
                new com.devmetrics.config.AppProperties.Scoring(90, 0.5)));
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    private User existingUser(String rawPassword) {
        User user = User.create("dev@devmetrics.com", passwordEncoder.encode(rawPassword),
                "Dev", "America/Sao_Paulo");
        user.setId(42L);
        return user;
    }

    @Test
    @DisplayName("registro guarda a senha com hash, nunca em texto puro")
    void registerHashesPassword() {
        when(userRepository.existsByEmailIgnoreCase("dev@devmetrics.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(new RegisterRequest("dev@devmetrics.com", "senha1234",
                "Dev", "America/Sao_Paulo"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("senha1234");
        assertThat(passwordEncoder.matches("senha1234", captor.getValue().getPasswordHash())).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("e-mail ja cadastrado devolve conflito")
    void duplicatedEmailIsRejected() {
        when(userRepository.existsByEmailIgnoreCase("dev@devmetrics.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("dev@devmetrics.com",
                "senha1234", "Dev", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_USED);
    }

    @Test
    @DisplayName("senha errada e e-mail inexistente devolvem exatamente o mesmo erro")
    void wrongPasswordAndUnknownEmailAreIndistinguishable() {
        when(userRepository.findByEmailIgnoreCase("dev@devmetrics.com"))
                .thenReturn(Optional.of(existingUser("senha1234")));
        when(userRepository.findByEmailIgnoreCase("ninguem@devmetrics.com"))
                .thenReturn(Optional.empty());

        BusinessException wrongPassword = catchBusinessException(() ->
                authService.login(new LoginRequest("dev@devmetrics.com", "senha-errada")));
        BusinessException unknownEmail = catchBusinessException(() ->
                authService.login(new LoginRequest("ninguem@devmetrics.com", "senha1234")));

        assertThat(wrongPassword.errorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(unknownEmail.errorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
    }

    @Test
    @DisplayName("login correto emite access e refresh token")
    void successfulLoginIssuesTokens() {
        when(userRepository.findByEmailIgnoreCase("dev@devmetrics.com"))
                .thenReturn(Optional.of(existingUser("senha1234")));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(new LoginRequest("dev@devmetrics.com", "senha1234"));

        assertThat(jwtService.parseAccessToken(response.accessToken())).isPresent();
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    @DisplayName("refresh rotaciona: o token usado e revogado na hora")
    void refreshRotatesToken() {
        User user = existingUser("senha1234");
        String raw = jwtService.generateRefreshToken();
        RefreshToken stored = RefreshToken.issue(user, jwtService.hashRefreshToken(raw),
                Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(raw)))
                .thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.refresh(raw);

        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(response.refreshToken()).isNotEqualTo(raw);
    }

    @Test
    @DisplayName("reusar um refresh token ja rotacionado falha")
    void reusingRevokedRefreshTokenFails() {
        User user = existingUser("senha1234");
        String raw = jwtService.generateRefreshToken();
        RefreshToken stored = RefreshToken.issue(user, jwtService.hashRefreshToken(raw),
                Instant.now().plusSeconds(3600));
        stored.revoke();
        when(refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(raw)))
                .thenReturn(Optional.of(stored));

        assertThat(catchBusinessException(() -> authService.refresh(raw)).errorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("refresh token expirado falha")
    void expiredRefreshTokenFails() {
        User user = existingUser("senha1234");
        String raw = jwtService.generateRefreshToken();
        RefreshToken stored = RefreshToken.issue(user, jwtService.hashRefreshToken(raw),
                Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(raw)))
                .thenReturn(Optional.of(stored));

        assertThat(catchBusinessException(() -> authService.refresh(raw)).errorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private BusinessException catchBusinessException(Runnable action) {
        try {
            action.run();
        } catch (BusinessException exception) {
            return exception;
        }
        throw new AssertionError("Esperava BusinessException");
    }
}
