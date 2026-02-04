package io.github.maksim0840.apigateway.exception;

public class DataNotFoundException extends RuntimeException {
    public DataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataNotFoundException(String message) {
        super(message);
    }
}
