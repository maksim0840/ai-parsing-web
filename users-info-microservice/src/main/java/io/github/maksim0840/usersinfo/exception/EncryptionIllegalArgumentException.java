package io.github.maksim0840.usersinfo.exception;

public class EncryptionIllegalArgumentException extends EncryptionException {
    public EncryptionIllegalArgumentException(String message) {
        super(message);
    }

    public EncryptionIllegalArgumentException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
