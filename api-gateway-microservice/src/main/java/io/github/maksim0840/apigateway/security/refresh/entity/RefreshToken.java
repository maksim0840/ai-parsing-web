package io.github.maksim0840.apigateway.security.refresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RedisHash("refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String id;              // jti из токена — идентификатор записи

    @Indexed
    private Long userId;            // для поиска всех токенов пользователя

    private String tokenHash;       // SHA-256 от самого токена, не сам токен

    private String replacedByJti;   // null — токен активен

    private Instant createdAt;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttl;               // запись сама исчезнет по истечении
}