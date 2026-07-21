package io.github.maksim0840.apigateway.security;

import io.github.maksim0840.internalapi.user.v1.dto.UserDTO;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final Duration accessLifetimeMin;
    private final Duration refreshLifetimeMin;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access_expiration_min}") long accessLifetime,
            @Value("${security.jwt.refresh_expiration_min}") long refreshLifetime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessLifetimeMin = Duration.ofMinutes(accessLifetime);
        this.refreshLifetimeMin = Duration.ofMinutes(refreshLifetime);
    }

    public String generateToken(UserDTO user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessLifetimeMin);

        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("name", user.name())
                .claim("role", user.role().name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public String extractName(String token) {
        return extractClaims(token).get("name", String.class);
    }

    public UserRole extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        return UserRole.valueOf(role);
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String generateRefreshToken(UserDTO user, String jti) {
        Instant now = Instant.now();

        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(user.id()))
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshLifetimeMin)))
                .signWith(secretKey)
                .compact();
    }

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    public String extractType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    public long getRefreshLifetimeSec() {
        return refreshLifetimeMin.toSeconds();
    }
}
