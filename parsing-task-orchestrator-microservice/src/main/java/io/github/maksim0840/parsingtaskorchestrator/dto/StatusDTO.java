package io.github.maksim0840.parsingtaskorchestrator.dto;

import io.github.maksim0840.internalapi.parsing_task_orchestrator.v1.enums.TaskStatus;

public record StatusDTO(
        TaskStatus status,
        String message
) {
}
