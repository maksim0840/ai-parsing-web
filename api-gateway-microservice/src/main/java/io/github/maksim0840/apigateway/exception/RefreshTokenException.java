package io.github.maksim0840.apigateway.exception;

/**
 * Refresh-токен невалиден, отозван или истёк.
 * Отличается от BadCredentialsException тем, что сигнализирует фронту
 * о необходимости полного перелогина, а не повторной попытки обновления.
 */
public class RefreshTokenException extends RuntimeException {
    public RefreshTokenException(String message) {
        super(message);
    }
}
