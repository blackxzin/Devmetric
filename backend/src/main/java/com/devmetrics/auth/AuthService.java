package com.devmetrics.auth;

import com.devmetrics.auth.domain.RefreshToken;
import com.devmetrics.auth.dto.AuthResponse;
import com.devmetrics.auth.dto.LoginRequest;
import com.devmetrics.auth.dto.RegisterRequest;
import com.devmetrics.auth.repository.RefreshTokenRepository;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.dto.UserResponse;
import com.devmetrics.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
        }
        User user = User.create(request.email(), passwordEncoder.encode(request.password()),
                request.displayName(), request.timezone());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            // Mensagem identica ao caso de e-mail inexistente: nao revela quais e-mails existem.
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!stored.isUsable()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        // Rotacao: o token usado morre aqui. Reuso de um token antigo falha.
        stored.revoke();
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void logoutAll(Long userId) {
        userRepository.findById(userId).ifPresent(user ->
                refreshTokenRepository.revokeAllForUser(user, Instant.now()));
    }

    private AuthResponse issueTokens(User user) {
        AuthenticatedUser principal = AuthenticatedUser.from(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefresh = jwtService.generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.issue(user,
                jwtService.hashRefreshToken(rawRefresh), jwtService.refreshExpiration()));
        return new AuthResponse(accessToken, rawRefresh, jwtService.accessTtlSeconds(),
                UserResponse.from(user));
    }
}
