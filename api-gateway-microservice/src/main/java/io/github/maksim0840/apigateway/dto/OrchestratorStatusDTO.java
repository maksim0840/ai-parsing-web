package io.github.maksim0840.apigateway.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import lombok.Builder;

@Builder
public record OrchestratorStatusDTO(
        String taskId,
        TaskStatus status,
        String message
) {
}
