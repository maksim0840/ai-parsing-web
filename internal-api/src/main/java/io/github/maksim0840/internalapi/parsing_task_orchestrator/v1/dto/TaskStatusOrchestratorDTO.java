package io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;
import lombok.Builder;

import javax.annotation.Nullable;

@Builder
@Nullable
public record TaskStatusOrchestratorDTO(
        String taskId,
        TaskStatus status,
        String message
) {
}
