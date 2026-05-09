package io.github.maksim0840.apigateway.dto.api;

public record TaskStatusResponse(
        String taskId,
        TaskStatus taskStatus
) {
}
