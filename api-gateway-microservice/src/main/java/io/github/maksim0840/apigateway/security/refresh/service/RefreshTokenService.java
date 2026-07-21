package io.github.maksim0840.apigateway.security.refresh.service;

import io.github.maksim0840.apigateway.security.refresh.entity.RefreshToken;
import io.github.maksim0840.apigateway.security.refresh.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Сохраняет выданный refresh-токен.
    public void save(String jti, Long userId, String rawToken, long ttlSec) {
        RefreshToken entity = RefreshToken.builder()
                .id(jti)
                .userId(userId)
                .tokenHash(hash(rawToken))
                .createdAt(Instant.now())
                .ttl(ttlSec)
                .build();

        refreshTokenRepository.save(entity);
    }

    /**
     * Проверяет, что токен с таким jti существует в хранилище
     * и его хеш совпадает с переданным токеном.
     * Несовпадение хеша означает, что подпись валидна, но конкретно этот
     * экземпляр токена уже был заменён при ротации — доверять ему нельзя.
     */
    public boolean isActive(String jti, String rawToken) {
        Optional<RefreshToken> stored = refreshTokenRepository.findById(jti);
        if (stored.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                stored.get().getTokenHash().getBytes(StandardCharsets.UTF_8),
                hash(rawToken).getBytes(StandardCharsets.UTF_8)
        );
    }

    // Отзывает один токен — используется при логауте и при ротации.
    public void revoke(String jti) {
        refreshTokenRepository.deleteById(jti);
    }

    // Отзывает все токены пользователя — "выйти со всех устройств" (смена пароля, блокировка аккаунта)
    public void revokeAll(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        refreshTokenRepository.deleteAll(tokens);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    // Помечает токен использованным и укорачивает TTL до окна догоняющих запросов
    public void markReplaced(String oldJti, String newJti) {
        refreshTokenRepository.findById(oldJti).ifPresent(token -> {
            token.setReplacedByJti(newJti);
            token.setTtl(30L);
            refreshTokenRepository.save(token);
        });
    }

    // true, если токен уже ротирован, но окно ещё не закрылось
    public boolean isWithinGraceWindow(String jti) {
        return refreshTokenRepository.findById(jti)
                .map(t -> t.getReplacedByJti() != null)
                .orElse(false);
    }
}
