package com.devmetrics.auth;

import com.devmetrics.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Emissao e validacao de tokens. O access token e um JWT assinado (HS256).
 * O refresh token e um valor aleatorio opaco; apenas o hash dele vai para o banco.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public JwtService(AppProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(properties.jwt().accessTtlMinutes());
        this.refreshTtl = Duration.ofDays(properties.jwt().refreshTtlDays());
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("name", user.displayName())
                .claim("tz", user.timezone())
                .claim("role", user.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("name", String.class),
                    claims.get("tz", String.class),
                    claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", ex);
        }
    }

    public Instant refreshExpiration() {
        return Instant.now().plus(refreshTtl);
    }

    public long accessTtlSeconds() {
        return accessTtl.getSeconds();
    }
}
