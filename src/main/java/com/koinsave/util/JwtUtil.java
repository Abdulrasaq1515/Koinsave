package com.koinsave.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultFallbackSecretKeyThatIsLongEnoughForTesting123!}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        try {
            String effectiveSecret = secret;
            if (effectiveSecret.length() < 32) {
                log.warn("JWT secret is too short ({} chars), using extended version", effectiveSecret.length());
                effectiveSecret = effectiveSecret + "EXTEND".repeat(10);
                effectiveSecret = effectiveSecret.substring(0, 64);
            }

            byte[] keyBytes = effectiveSecret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to create JWT signing key", e);
            throw new RuntimeException("JWT configuration error", e);
        }
    }

    public String generateToken(String email, Long userId) {
        try {
            log.debug("Generating JWT token for email: {}, userId: {}", email, userId);

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            String token = Jwts.builder()
                    .subject(email)
                    .claim("userId", userId)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getSigningKey())
                    .compact();

            log.debug("JWT token generated successfully");
            return token;

        } catch (Exception e) {
            log.error("Failed to generate JWT token for email: {}", email, e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public Optional<String> extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Long> extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.get("userId", Long.class));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}