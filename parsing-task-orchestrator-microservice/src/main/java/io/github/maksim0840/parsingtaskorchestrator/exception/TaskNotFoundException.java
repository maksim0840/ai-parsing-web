package io.github.maksim0840.parsingtaskorchestrator.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }

    public TaskNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }
}