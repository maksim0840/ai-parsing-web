package io.github.maksim0840.usersinfo.exception;

public class EncryptionException extends RuntimeException {
    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
