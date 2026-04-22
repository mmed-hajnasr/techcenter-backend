package com.isi.techcenter_backend.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.isi.techcenter_backend.auth.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final String issuer;

    public JwtService(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        long expirationSeconds = jwtProperties.getExpirationSeconds();
        String issuer = jwtProperties.getIssuer();

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateAccessToken(UUID userId, UserRole role) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public Optional<AuthenticatedUser> extractAuthenticatedUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getSubject() == null) {
                return Optional.empty();
            }

            UUID userId = UUID.fromString(claims.getSubject());
            String roleValue = claims.get("role", String.class);
            if (roleValue == null) {
                return Optional.empty();
            }
            UserRole role = UserRole.valueOf(roleValue);

            return Optional.of(new AuthenticatedUser(userId, role));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public record AuthenticatedUser(UUID userId, UserRole role) {
    }
}
