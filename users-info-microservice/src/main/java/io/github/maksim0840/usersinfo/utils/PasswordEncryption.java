package io.github.maksim0840.usersinfo.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Класс для хэширования и проверки паролей.
 * Используется алгоритм BCrypt, специализирующийся на безопасном хранении паролей.
 */
public class PasswordEncryption {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    public static String makeHash(String password) {
        return ENCODER.encode(password);
    }

    public static boolean checkMatching(String rawPassword, String passwordHash) {
        return ENCODER.matches(rawPassword, passwordHash);
    }
}
