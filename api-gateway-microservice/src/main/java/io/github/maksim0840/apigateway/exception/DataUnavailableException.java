package io.github.maksim0840.apigateway.exception;

public class DataUnavailableException extends RuntimeException {
    public DataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataUnavailableException(String message) {
        super(message);
    }
}
